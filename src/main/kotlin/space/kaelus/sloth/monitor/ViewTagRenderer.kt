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
package space.kaelus.sloth.monitor

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import org.bukkit.entity.Player
import space.kaelus.sloth.checks.impl.ai.AiCheck
import space.kaelus.sloth.player.PlayerDataManager

internal class ViewTagRenderer(private val playerDataManager: PlayerDataManager) {
  fun render(target: Player, state: TargetTeamState, config: ViewRuntimeConfig): RenderedTag {
    val slothTarget = playerDataManager.getPlayer(target)
    if (slothTarget == null) {
      val fallbackValues =
        mapOf(
          "prob" to config.fallbackProb,
          "buffer" to config.fallbackBuffer,
          "ping" to state.resolvePingDisplay(target.ping, config),
        )
      return RenderedTag(
        applyTemplate(config.prefixTemplate, fallbackValues),
        applyTemplate(config.suffixTemplate, fallbackValues),
        applyTemplate(config.belowTemplate, fallbackValues),
        ZERO_BELOW_SCORE,
      )
    }
    val aiCheck = slothTarget.checkManager.getCheck(AiCheck::class.java)

    val probabilityValue =
      if (aiCheck == null) {
        config.fallbackProb
      } else {
        formatDecimal(aiCheck.lastProbability * PERCENT_MULTIPLIER, config.probDecimals)
      }
    val belowScore =
      if (aiCheck == null) {
        ZERO_BELOW_SCORE
      } else {
        (aiCheck.lastProbability * PERCENT_MULTIPLIER).roundToInt().coerceAtLeast(ZERO_BELOW_SCORE)
      }

    val bufferValue =
      if (aiCheck == null) {
        config.fallbackBuffer
      } else {
        formatDecimal(aiCheck.buffer, config.bufferDecimals)
      }

    val values =
      mapOf(
        "prob" to probabilityValue,
        "buffer" to bufferValue,
        "ping" to state.resolvePingDisplay(target.ping, config),
      )

    return RenderedTag(
      applyTemplate(config.prefixTemplate, values),
      applyTemplate(config.suffixTemplate, values),
      applyTemplate(config.belowTemplate, values),
      belowScore,
    )
  }

  private fun applyTemplate(template: String, values: Map<String, String>): String {
    return renderViewTemplate(template, values)
  }

  private fun formatDecimal(value: Double, decimals: Int): String {
    val safeDecimals = decimals.coerceAtLeast(0)
    val normalized = if (abs(value) < DECIMAL_EPSILON) 0.0 else value
    return String.format(Locale.US, "%.${safeDecimals}f", normalized)
  }

  private companion object {
    const val ZERO_BELOW_SCORE = 0
    const val PERCENT_MULTIPLIER = 100.0
    const val DECIMAL_EPSILON = 0.0000001
  }
}
