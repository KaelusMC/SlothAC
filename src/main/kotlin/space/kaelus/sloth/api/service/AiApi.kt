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
import org.bukkit.entity.Player
import space.kaelus.sloth.api.model.AiSnapshot

/** Access to AI status and the latest prediction snapshot per player. */
interface AiApi {
  /** Returns true when AI integration is enabled. */
  fun isEnabled(): Boolean

  /**
   * Latest AI snapshot for a player, if available.
   *
   * @param playerId player UUID
   * @return snapshot with probability/buffer/dmg/prob90
   */
  fun getSnapshot(playerId: UUID): Optional<AiSnapshot>

  /** Convenience overload for Bukkit [Player]. */
  fun getSnapshot(player: Player?): Optional<AiSnapshot> {
    if (player == null) {
      return Optional.empty()
    }
    return getSnapshot(player.uniqueId)
  }
}
