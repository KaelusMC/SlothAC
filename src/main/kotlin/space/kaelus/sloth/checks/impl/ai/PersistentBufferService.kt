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

import java.util.Locale
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.database.DatabaseManager
import space.kaelus.sloth.debug.DebugCategory
import space.kaelus.sloth.debug.DebugManager
import space.kaelus.sloth.player.SlothPlayer
import space.kaelus.sloth.scheduler.SchedulerService

private const val MILLIS_PER_HOUR = 3_600_000.0

class PersistentBufferService(
  private val configManager: ConfigManager,
  private val databaseManager: DatabaseManager,
  private val scheduler: SchedulerService,
  private val debugManager: DebugManager,
  private val logger: Logger,
) {
  fun restoreOnLogin(slothPlayer: SlothPlayer) {
    if (!configManager.persistentBufferEnabled) return
    val aiCheck = slothPlayer.checkManager.getCheck(AiCheck::class.java) ?: return

    scheduler.runAsync {
      val state = databaseManager.database.loadAiBuffer(slothPlayer.uuid) ?: return@runAsync
      val now = System.currentTimeMillis()
      val ageMillis = now - state.updatedAt
      val playerName = slothPlayer.player.name

      if (ageMillis < 0L) {
        logger.warning(
          "[PersistentBuffer] Skipped restore for $playerName: stored timestamp is in the future"
        )
        return@runAsync
      }

      if (!slothPlayer.player.isOnline) return@runAsync

      if (ageMillis < configManager.persistentBufferDisconnectWindowMillis) {
        scheduler.runSync(slothPlayer.player) {
          if (slothPlayer.player.isOnline) aiCheck.restoreBuffer(state.buffer)
        }
        debugManager.log(
          DebugCategory.AI_PERSISTENT_BUFFER,
          "$playerName reconnected within disconnect window; buffer ${format(state.buffer)} kept",
        )
        return@runAsync
      }

      if (ageMillis > configManager.persistentBufferTtlMillis) {
        debugManager.log(
          DebugCategory.AI_PERSISTENT_BUFFER,
          "$playerName buffer expired (offline ${format(ageMillis / MILLIS_PER_HOUR)}h), discarded",
        )
        return@runAsync
      }

      val ageHours = ageMillis / MILLIS_PER_HOUR
      val decayed = state.buffer - configManager.persistentBufferDecayPerHour * ageHours
      val capped = min(decayed, configManager.persistentBufferCap)
      val finalBuffer = max(0.0, capped)

      scheduler.runSync(slothPlayer.player) {
        if (slothPlayer.player.isOnline) aiCheck.restoreBuffer(finalBuffer)
      }
      debugManager.log(
        DebugCategory.AI_PERSISTENT_BUFFER,
        "$playerName restored buffer ${format(state.buffer)} → ${format(finalBuffer)} (offline ${format(ageHours)}h)",
      )
    }
  }

  fun saveOnQuit(slothPlayer: SlothPlayer) {
    val buffer = bufferToPersist(slothPlayer) ?: return
    databaseManager.database.saveAiBuffer(slothPlayer.uuid, buffer, System.currentTimeMillis())
  }

  fun saveOnShutdown(slothPlayer: SlothPlayer) {
    saveOnQuit(slothPlayer)
  }

  private fun bufferToPersist(slothPlayer: SlothPlayer): Double? {
    val buffer = slothPlayer.checkManager.getCheck(AiCheck::class.java)?.buffer
    return when {
      !configManager.persistentBufferEnabled -> null
      buffer == null || buffer < configManager.persistentBufferSaveThreshold -> null
      else -> buffer
    }
  }

  private fun format(value: Double): String = String.format(Locale.US, "%.2f", value)
}
