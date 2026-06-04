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
package space.kaelus.sloth.config

import java.io.File

internal object ConfigMigrations {
  const val LATEST_VERSION = 2

  private val VERSION_RE = Regex("""^\s*config-version:\s*(\d+)""", RegexOption.MULTILINE)

  fun readVersion(file: File): Int {
    val text = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: return LATEST_VERSION
    return VERSION_RE.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
  }

  // yaml-config-updater uses `/` as the path separator because `.` is valid inside key names.
  fun forcedDropsForUpgradeFrom(currentVersion: Int): List<String> {
    if (currentVersion >= LATEST_VERSION) return emptyList()
    val drops = mutableListOf("config-version")
    // if (currentVersion < 2) drops += "ai/legacy-path"
    return drops
  }
}
