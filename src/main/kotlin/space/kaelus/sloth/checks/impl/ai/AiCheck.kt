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
package space.kaelus.sloth.checks.impl.ai

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying
import java.util.ArrayDeque
import java.util.Arrays
import java.util.concurrent.atomic.AtomicReference
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.ai.AiResult
import space.kaelus.sloth.ai.AiService
import space.kaelus.sloth.ai.AiServiceException
import space.kaelus.sloth.alert.AlertManager
import space.kaelus.sloth.alert.AlertType
import space.kaelus.sloth.api.event.AiPredictionEvent
import space.kaelus.sloth.checks.AbstractCheck
import space.kaelus.sloth.checks.CheckData
import space.kaelus.sloth.checks.CheckFactory
import space.kaelus.sloth.checks.Reloadable
import space.kaelus.sloth.checks.type.PacketCheck
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.damage.DamageProcessor
import space.kaelus.sloth.data.TickData
import space.kaelus.sloth.debug.DebugCategory
import space.kaelus.sloth.debug.DebugManager
import space.kaelus.sloth.player.SlothPlayer
import space.kaelus.sloth.region.RegionProvider
import space.kaelus.sloth.scheduler.SchedulerService
import space.kaelus.sloth.server.AIServer
import space.kaelus.sloth.utils.Message
import space.kaelus.sloth.utils.MessageUtil

@CheckData(name = "AI (Aim)")
class AiCheck(
  slothPlayer: SlothPlayer,
  private val plugin: SlothAC,
  private val aiService: AiService,
  private val configManager: ConfigManager,
  private val regionProvider: RegionProvider,
  private val alertManager: AlertManager,
  private val damageProcessor: DamageProcessor,
  private val debugManager: DebugManager,
  private val scheduler: SchedulerService,
) : AbstractCheck(slothPlayer), PacketCheck, Reloadable {
  private var step: Int = 0
  private var aiEnabled = false
  private var ticks: ArrayDeque<TickData> = ArrayDeque()
  private val snapshotBuffer: AtomicReference<Array<TickData?>?> = AtomicReference()
  private var ticksStep = 0

  var buffer: Double = 0.0
    private set

  var lastProbability: Double = 0.0
    private set

  var prob90: Int = 0

  private var flag = 0.0
  private var bufferResetOnFlag = 0.0
  private var bufferMultiplier = 0.0
  private var bufferDecrease = 0.0
  private var suspiciousAlertBuffer = 0.0

  init {
    reload()
  }

  interface Factory : CheckFactory {
    override fun create(player: SlothPlayer): AiCheck
  }

  override fun reload() {
    aiEnabled = aiService.isEnabled

    if (ticks.isEmpty() || ticks.size != configManager.aiSequence) {
      ticks = ArrayDeque(configManager.aiSequence)
    }

    step = configManager.aiStep
    flag = configManager.aiFlag
    bufferResetOnFlag = configManager.aiResetOnFlag
    bufferMultiplier = configManager.aiBufferMultiplier
    bufferDecrease = configManager.aiBufferDecrease
    suspiciousAlertBuffer = configManager.suspiciousAlertsBuffer
  }

  override fun onPacketReceive(event: PacketReceiveEvent) {
    if (!aiEnabled) return
    if (!WrapperPlayClientPlayerFlying.isFlying(event.packetType)) return
    val slothPlayer = slothPlayer

    val sequence = configManager.aiSequence

    if (slothPlayer.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
      debugManager.log(
        DebugCategory.PACKET_DUPLICATION,
        "Mojang failed IQ Test for: ${slothPlayer.player.name}.",
      )
      return
    }

    if (
      slothPlayer.packetStateData.lastPacketWasTeleport ||
        slothPlayer.packetStateData.lastPacketWasServerRotation
    ) {
      return
    }

    if (slothPlayer.combat.ticksSinceAttack > sequence) {
      if (ticks.isNotEmpty()) {
        ticks.clear()
      }
      ticksStep = 0
      return
    }

    ticks.addLast(TickData(slothPlayer))
    ticksStep++

    while (ticks.size > sequence) {
      ticks.removeFirst()
    }

    if (ticks.size == sequence && ticksStep >= step) {
      if (
        configManager.isAiWorldGuardEnabled() &&
          regionProvider.isPlayerInDisabledRegion(slothPlayer.player)
      ) {
        debugManager.log(
          DebugCategory.WORLDGUARD,
          "Player ${slothPlayer.player.name} is in a disabled region. Skipping AI check.",
        )
        ticksStep = 0
        return
      }
      sendData()
      ticksStep = 0
    }
  }

  private fun sendData() {
    if (ticks.isEmpty() || !aiEnabled) {
      return
    }

    val slothPlayer = slothPlayer
    val count = ticks.size
    val snapshot = borrowSnapshot(count)
    var index = 0
    for (tick in ticks) {
      snapshot[index++] = tick
    }

    val player = slothPlayer.player
    val playerName = player.name

    scheduler.runAsync {
      try {
        @Suppress("UNCHECKED_CAST") val requestTicks = snapshot as Array<TickData>
        aiService
          .request(requestTicks, count)
          .thenAcceptAsync({ parsed -> onResponse(parsed) }) { runnable ->
            scheduler.runSync(player, runnable)
          }
          .exceptionally { error ->
            scheduler.runSync(player, Runnable { onError(error) })
            null
          }
      } catch (e: Exception) {
        plugin.logger.warning("[AiCheck] Failed to send data for $playerName: ${e.message}")
      } finally {
        releaseSnapshot(snapshot, count)
      }
    }
  }

  private fun onResponse(parsed: AiResult) {
    val slothPlayer = slothPlayer
    if (parsed.disabled) {
      lastProbability = 0.0
      damageProcessor.reset(slothPlayer)
      return
    }

    if (parsed.hasParseError()) {
      plugin.logger.warning(
        "[AiCheck] Error parsing API response: ${parsed.parseError?.message}. Response Body: ${parsed.raw}"
      )
      lastProbability = 0.0
      damageProcessor.reset(slothPlayer)
      return
    }

    val apiResponse = parsed.response

    if (apiResponse == null) {
      plugin.logger.warning(
        "[AiCheck] API response is missing probability. Response: ${parsed.raw}"
      )
      lastProbability = 0.0
      damageProcessor.reset(slothPlayer)
      return
    }

    val probability = apiResponse.probability
    lastProbability = probability
    damageProcessor.applyProbability(slothPlayer, probability)

    if (probability > 0.9) {
      prob90++
    }

    val oldBuffer = buffer

    if (probability > CHEAT_PROBABILITY) {
      buffer += (probability - CHEAT_PROBABILITY) * bufferMultiplier
    } else if (probability < LEGIT_PROBABILITY) {
      buffer = kotlin.math.max(0.0, buffer - bufferDecrease)
    }

    if (buffer > suspiciousAlertBuffer && oldBuffer <= suspiciousAlertBuffer) {
      alertManager.send(
        MessageUtil.getMessage(
          Message.SUSPICIOUS_ALERT_TRIGGERED,
          "player",
          slothPlayer.player.name,
          "buffer",
          String.format("%.1f", buffer),
        ),
        AlertType.SUSPICIOUS,
      )
    }

    debugManager.log(
      DebugCategory.AI_PROBABILITY,
      String.format(
        "[%s] Prob: %.4f | Buffer: %.2f -> %.2f | Damage Multiplier: %.2f",
        slothPlayer.player.name,
        probability,
        oldBuffer,
        buffer,
        slothPlayer.combat.damageMultiplier,
      ),
    )

    var flagged = false
    if (buffer > flag) {
      flagged = true
      flag("prob=${String.format("%.2f", probability)} buffer=${String.format("%.1f", buffer)}")
      buffer = bufferResetOnFlag
    }

    slothPlayer.eventBus.post(
      AiPredictionEvent(
        slothPlayer.uuid,
        slothPlayer.player.name,
        checkName,
        probability,
        oldBuffer,
        buffer,
        slothPlayer.combat.damageMultiplier,
        prob90,
        flagged,
      )
    )
  }

  private fun onError(error: Throwable): Void? {
    lastProbability = 0.0
    val slothPlayer = slothPlayer
    damageProcessor.reset(slothPlayer)

    val cause = (error as? java.util.concurrent.CompletionException)?.cause ?: error

    val newSequence = (cause as? AiServiceException)?.newSequence
    if (newSequence != null) {
      if (configManager.aiSequence != newSequence) {
        plugin.logger.info("[AiCheck] Received new sequence length $newSequence")
        configManager.aiSequence = newSequence
        ticks = ArrayDeque(newSequence)
      }
      return null
    }

    if (cause is AIServer.RequestException) {
      if (cause.code == AIServer.ResponseCode.WAITING) {
        return null
      }

      val logMessage =
        "[AiCheck] API Error ${cause.code} for player ${slothPlayer.player.name}: ${cause.message}"

      if (cause.code == AIServer.ResponseCode.TIMEOUT) {
        debugManager.log(DebugCategory.AI_TIMEOUT, logMessage)
      } else {
        plugin.logger.warning(logMessage)
      }
    } else {
      plugin.logger.warning(
        "[AiCheck] Unknown API Error for ${slothPlayer.player.name}: ${cause.message}"
      )
    }
    return null
  }

  private fun borrowSnapshot(size: Int): Array<TickData?> {
    val buffer = snapshotBuffer.getAndSet(null)
    if (buffer == null || buffer.size < size) {
      return arrayOfNulls(size)
    }
    return buffer
  }

  private fun releaseSnapshot(buffer: Array<TickData?>, used: Int) {
    Arrays.fill(buffer, 0, used, null)
    snapshotBuffer.set(buffer)
  }

  companion object {
    private const val CHEAT_PROBABILITY = 0.90
    private const val LEGIT_PROBABILITY = 0.10
  }
}
