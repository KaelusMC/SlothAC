/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2026 KaelusMC
 *
 * SlothAC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SlothAC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package space.kaelus.sloth.server

import java.util.function.Supplier
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.ai.AiTransport
import space.kaelus.sloth.ai.BatchingAiTransport
import space.kaelus.sloth.ai.RetryExecutor
import space.kaelus.sloth.ai.RetryingAiBatchTransport
import space.kaelus.sloth.ai.RetryingAiTransport
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.scheduler.SchedulerService

class AIServerProvider(
  private val plugin: SlothAC,
  private val configManager: ConfigManager,
  private val scheduler: SchedulerService,
) : Supplier<AiTransport?> {
  @Volatile private var currentTransport: AiTransport? = null
  @Volatile private var batchingTransport: BatchingAiTransport? = null
  @Volatile private var apiCooldown: ApiCooldown? = null

  init {
    reload()
  }

  fun reload() {
    shutdown()
    apiCooldown = buildCooldown()
    currentTransport = buildTransport()
  }

  fun shutdown() {
    batchingTransport?.stop()
    batchingTransport = null
    currentTransport = null
  }

  fun shutdownTransport() {
    shutdown()
    AIServer.shutdownHttpClient()
  }

  override fun get(): AiTransport? = currentTransport

  private fun buildTransport(): AiTransport? {
    val server = buildServer() ?: return null
    return if (configManager.batchEnabled) wrapWithBatching(server) else wrapWithRetry(server)
  }

  private fun buildServer(): AIServer? {
    val url = configManager.aiServerUrl
    val key = configManager.aiApiKey
    val state =
      when {
        !configManager.isAiEnabled() -> ServerState.DISABLED
        url.isBlank() || url == LEGACY_PLACEHOLDER_URL || key == "API-KEY" ->
          ServerState.NOT_CONFIGURED
        else -> ServerState.READY
      }
    return when (state) {
      ServerState.DISABLED -> {
        plugin.logger.info("[AiCheck] AI Check disabled.")
        null
      }
      ServerState.NOT_CONFIGURED -> {
        plugin.logger.warning("[AiCheck] AI is enabled but not configured.")
        null
      }
      ServerState.READY -> {
        plugin.logger.info("[AiCheck] AI Check loaded.")
        AIServer(plugin, url, key, apiCooldown!!)
      }
    }
  }

  private enum class ServerState {
    DISABLED,
    NOT_CONFIGURED,
    READY,
  }

  private companion object {
    // Old config.yml placeholder URL - treat as not-configured.
    const val LEGACY_PLACEHOLDER_URL = "https://url/v1/inference"
  }

  private fun wrapWithRetry(server: AIServer): AiTransport =
    RetryingAiTransport(server, newRetryExecutor())

  private fun wrapWithBatching(server: AIServer): AiTransport {
    val retriedBatch = RetryingAiBatchTransport(server, newRetryExecutor())
    val retriedSingle = RetryingAiTransport(server, newRetryExecutor())
    val batching =
      BatchingAiTransport(
        batchTransport = retriedBatch,
        singleTransport = retriedSingle,
        scheduler = scheduler,
        logger = plugin.logger,
        config =
          BatchingAiTransport.BatchConfig(
            maxBatchSize = configManager.batchMaxSize.coerceIn(1, AIServer.BATCH_MAX_ITEMS),
            maxDelayMs = configManager.batchMaxDelayMs.coerceAtLeast(1L),
          ),
      )
    batching.start()
    batchingTransport = batching
    return batching
  }

  private fun newRetryExecutor(): RetryExecutor = RetryExecutor(scheduler, buildRetryConfig())

  private fun buildCooldown(): ApiCooldown {
    val initialDuration = configManager.config.getLong("ai.backoff.initial-duration", 5)
    val maxDuration = configManager.config.getLong("ai.backoff.max-duration", 60)
    val multiplier = configManager.config.getDouble("ai.backoff.multiplier", 2.0)
    return ApiCooldown(initialDuration, maxDuration, multiplier)
  }

  private fun buildRetryConfig(): RetryExecutor.RetryConfig =
    RetryExecutor.RetryConfig(
      maxAttempts = configManager.retryMaxAttempts.coerceAtLeast(1),
      initialDelayMs = configManager.retryInitialDelayMs.coerceAtLeast(0L),
      maxDelayMs = configManager.retryMaxDelayMs.coerceAtLeast(0L),
      multiplier = configManager.retryMultiplier.coerceAtLeast(1.0),
      jitter = configManager.retryJitter.coerceIn(0.0, 1.0),
    )
}
