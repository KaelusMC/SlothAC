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
package space.kaelus.sloth.player

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.entity.Player

class ExemptManager {
  private val temporaryExemptions = ConcurrentHashMap<UUID, Long>()

  fun isExempt(player: Player?): Boolean {
    if (player == null) {
      return false
    }
    if (player.hasPermission("sloth.exempt")) {
      return true
    }

    val expiryTime = temporaryExemptions[player.uniqueId] ?: return false
    if (expiryTime != -1L && System.currentTimeMillis() > expiryTime) {
      temporaryExemptions.remove(player.uniqueId)
      return false
    }
    return true
  }

  fun addExemption(uuid: UUID, durationMillis: Long) {
    if (durationMillis == -1L) {
      temporaryExemptions[uuid] = -1L
      return
    }
    val expiryTime = System.currentTimeMillis() + durationMillis
    temporaryExemptions[uuid] = expiryTime
  }

  fun removeExemption(uuid: UUID): Boolean {
    return temporaryExemptions.remove(uuid) != null
  }

  fun getExpiryTime(uuid: UUID): Long? {
    return temporaryExemptions[uuid]
  }
}
