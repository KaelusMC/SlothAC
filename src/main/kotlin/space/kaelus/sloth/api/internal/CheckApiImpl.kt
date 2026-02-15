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
package space.kaelus.sloth.api.internal

import java.util.Locale
import java.util.Optional
import java.util.UUID
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import space.kaelus.sloth.api.model.CheckInfo
import space.kaelus.sloth.api.service.CheckApi
import space.kaelus.sloth.checks.AbstractCheck
import space.kaelus.sloth.checks.ICheck
import space.kaelus.sloth.player.PlayerDataManager

class CheckApiImpl(private val playerDataManager: PlayerDataManager) : CheckApi {
  override fun listChecks(playerId: UUID): ImmutableList<CheckInfo> {
    val player = playerDataManager.getPlayer(playerId) ?: return persistentListOf()
    val builder = persistentListOf<CheckInfo>().builder()
    for (check in player.checkManager.getAllChecks()) {
      builder.add(toInfo(check))
    }
    return builder.build()
  }

  override fun getCheck(playerId: UUID, checkName: String): Optional<CheckInfo> {
    if (checkName.isBlank()) {
      return Optional.empty()
    }
    val player = playerDataManager.getPlayer(playerId) ?: return Optional.empty()
    val needle = checkName.trim().lowercase(Locale.ROOT)
    for (check in player.checkManager.getAllChecks()) {
      val info = toInfo(check)
      if (info.name.lowercase(Locale.ROOT) == needle) {
        return Optional.of(info)
      }
      if (info.configName != null && info.configName.lowercase(Locale.ROOT) == needle) {
        return Optional.of(info)
      }
    }
    return Optional.empty()
  }

  private fun toInfo(check: ICheck): CheckInfo {
    val configName =
      if (check is AbstractCheck) {
        check.configName
      } else {
        null
      }
    return CheckInfo(check.checkName, configName)
  }
}
