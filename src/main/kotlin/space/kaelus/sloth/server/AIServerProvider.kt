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
import space.kaelus.sloth.config.ConfigManager

class AIServerProvider(private val plugin: SlothAC, private val configManager: ConfigManager) :
  Supplier<AIServer?> {
  private var currentInstance: AIServer? = null
  private var apiCooldown: ApiCooldown? = null

  init {
    reload()
  }

  fun reload() {
    val initialDuration = configManager.config.getLong("ai.backoff.initial-duration", 5)
    val maxDuration = configManager.config.getLong("ai.backoff.max-duration", 60)
    val multiplier = configManager.config.getDouble("ai.backoff.multiplier", 2.0)
    apiCooldown = ApiCooldown(initialDuration, maxDuration, multiplier)
    if (configManager.isAiEnabled()) {
      val url = configManager.aiServerUrl
      val key = configManager.aiApiKey

      if (url.isBlank() || key == "API-KEY") {
        plugin.logger.warning("[AiCheck] AI is enabled but not configured.")
        currentInstance = null
      } else {
        plugin.logger.info("[AiCheck] AI Check loaded.")
        currentInstance = AIServer(plugin, url, key, apiCooldown!!)
      }
    } else {
      plugin.logger.info("[AiCheck] AI Check disabled.")
      currentInstance = null
    }
  }

  override fun get(): AIServer? = currentInstance
}
