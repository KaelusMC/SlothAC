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
package space.kaelus.sloth.debug

import java.util.EnumSet
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.config.ConfigManager

class DebugManager(private val plugin: SlothAC, private val configManager: ConfigManager) {
  private val enabledCategories: MutableSet<DebugCategory> =
    EnumSet.noneOf(DebugCategory::class.java)
  private var debugEnabled = false

  init {
    reload()
  }

  fun reload() {
    enabledCategories.clear()
    enabledCategories.addAll(configManager.enabledDebugCategories)
    debugEnabled = configManager.isDebugEnabled()
  }

  fun isEnabled(category: DebugCategory): Boolean {
    return debugEnabled && enabledCategories.contains(category)
  }

  fun log(category: DebugCategory, message: String) {
    if (isEnabled(category)) {
      plugin.logger.info("[DEBUG | ${category.name}] $message")
    }
  }
}
