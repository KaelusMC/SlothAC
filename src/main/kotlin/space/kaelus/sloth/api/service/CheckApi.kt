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
package space.kaelus.sloth.api.service

import java.util.Optional
import java.util.UUID
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.bukkit.entity.Player
import space.kaelus.sloth.api.model.CheckInfo

/** Access to per-player check list and metadata. */
interface CheckApi {
  /** Returns all checks created for a player. */
  fun listChecks(playerId: UUID): ImmutableList<CheckInfo>

  /** Returns a check by name or configName for a player. */
  fun getCheck(playerId: UUID, checkName: String): Optional<CheckInfo>

  /** Convenience overload for Bukkit [Player]. */
  fun listChecks(player: Player?): ImmutableList<CheckInfo> {
    if (player == null) {
      return persistentListOf()
    }
    return listChecks(player.uniqueId)
  }

  fun getCheck(player: Player?, checkName: String): Optional<CheckInfo> {
    if (player == null) {
      return Optional.empty()
    }
    return getCheck(player.uniqueId, checkName)
  }
}
