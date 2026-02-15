/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2026 KaelusMC
 *
 * This file contains code derived from GrimAC.
 * The original authors of GrimAC are credited below.
 *
 * Copyright (c) 2021-2026 GrimAC, DefineOutside and contributors.
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
package space.kaelus.sloth.database

import java.util.UUID
import space.kaelus.sloth.monitor.MonitorSettings
import space.kaelus.sloth.player.SlothPlayer

interface ViolationDatabase {
  fun logAlert(player: SlothPlayer, verbose: String, checkName: String, vls: Int)

  fun getLogCount(player: UUID): Int

  fun getViolations(player: UUID, page: Int, limit: Int): List<Violation>

  fun getUniqueViolatorsSince(since: Long): Int

  fun getLogCount(since: Long): Int

  fun getViolations(page: Int, limit: Int, since: Long): List<Violation>

  fun getViolationLevel(playerUUID: UUID, punishGroupName: String): Int

  fun incrementViolationLevel(playerUUID: UUID, punishGroupName: String): Int

  fun resetViolationLevel(playerUUID: UUID, punishGroupName: String)

  fun resetAllViolationLevels(playerUUID: UUID)

  fun loadMonitorSettings(playerUUID: UUID): MonitorSettings?

  fun saveMonitorSettings(playerUUID: UUID, settings: MonitorSettings)
}
