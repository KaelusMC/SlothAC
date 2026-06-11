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
package space.kaelus.sloth.redis

import com.fasterxml.jackson.databind.ObjectMapper
import java.util.logging.Level
import java.util.logging.Logger
import space.kaelus.sloth.checks.impl.ai.AiCheck
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.platform.scheduler.TaskHandle
import space.kaelus.sloth.player.PlayerDataManager
import space.kaelus.sloth.player.SlothPlayer
import space.kaelus.sloth.scheduler.SchedulerService

class CrossServerSuspiciousService(
  private val configManager: ConfigManager,
  private val redisManager: RedisManager,
  private val playerDataManager: PlayerDataManager,
  private val scheduler: SchedulerService,
  private val logger: Logger,
) {
  private val mapper = ObjectMapper()

  @Volatile private var enabled = false
  @Volatile private var refreshTask: TaskHandle? = null
  @Volatile
  var serverName: String = DEFAULT_SERVER_NAME
    private set

  private var keyPrefix = "$DEFAULT_CHANNEL:suspect"
  private var ttlSeconds = DEFAULT_TTL_SECONDS

  val isActive: Boolean
    get() = enabled

  fun start() {
    val config = configManager.config
    if (
      !config.getBoolean("cross-server.enabled", false) ||
        !config.getBoolean("cross-server.alerts.suspicious", true)
    ) {
      return
    }

    serverName = config.getString("cross-server.server-name", DEFAULT_SERVER_NAME)
    val channel = config.getString("cross-server.channel", DEFAULT_CHANNEL)
    keyPrefix = "$channel:suspect"
    ttlSeconds = config.getLong("cross-server.suspicious-sync.ttl-seconds", DEFAULT_TTL_SECONDS)
    val refreshSeconds =
      config
        .getLong("cross-server.suspicious-sync.refresh-seconds", DEFAULT_REFRESH_SECONDS)
        .coerceIn(1L, ttlSeconds.coerceAtLeast(1L))
    ttlSeconds = ttlSeconds.coerceAtLeast(refreshSeconds + 1L)

    redisManager.start()
    if (!redisManager.isAvailable) {
      logger.warning(
        "[CrossServer] suspicious-sync is enabled but Redis is unavailable; the list stays local."
      )
      return
    }

    enabled = true
    val periodTicks = refreshSeconds * TICKS_PER_SECOND
    refreshTask = scheduler.runTimer(::publishLocalSuspicious, periodTicks, periodTicks)
    logger.info(
      "[CrossServer] Sharing suspicious players as \"$serverName\" " +
        "(refresh ${refreshSeconds}s, ttl ${ttlSeconds}s)."
    )
  }

  private fun publishLocalSuspicious() {
    if (!enabled) return
    playerDataManager.getPlayers().forEach(::publishPlayer)
  }

  private fun publishPlayer(slothPlayer: SlothPlayer) {
    val check = slothPlayer.checkManager.getCheck(AiCheck::class.java)
    if (check == null || check.buffer <= 0.0) return
    val player = slothPlayer.player
    val payload =
      runCatching {
          mapper.writeValueAsString(
            SuspiciousSnapshot(
              server = serverName,
              uuid = slothPlayer.uuid.toString(),
              name = player.name,
              buffer = check.buffer,
              ping = player.ping,
              updatedAt = System.currentTimeMillis(),
            )
          )
        }
        .getOrNull() ?: return
    redisManager.setWithTtl("$keyPrefix:$serverName:${slothPlayer.uuid}", payload, ttlSeconds)
  }

  fun fetchRemote(): List<SuspiciousSnapshot> {
    if (!enabled) return emptyList()
    return redisManager
      .scanValues("$keyPrefix:*")
      .mapNotNull { raw ->
        runCatching { mapper.readValue(raw, SuspiciousSnapshot::class.java) }
          .onFailure { error ->
            logger.log(Level.FINE, "[CrossServer] Bad suspect payload.", error)
          }
          .getOrNull()
      }
      .filter { it.server != serverName }
  }

  fun shutdown() {
    enabled = false
    refreshTask?.cancel()
    refreshTask = null
  }

  private companion object {
    const val DEFAULT_SERVER_NAME = "server-1"
    const val DEFAULT_CHANNEL = "slothac:alerts"
    const val DEFAULT_TTL_SECONDS = 30L
    const val DEFAULT_REFRESH_SECONDS = 10L
    const val TICKS_PER_SECOND = 20L
  }
}
