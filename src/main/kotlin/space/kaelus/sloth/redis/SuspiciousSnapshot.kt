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
package space.kaelus.sloth.redis

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * One suspicious player, as shared across servers for the network-wide `/sloth suspicious` list.
 * Stored in Redis under a per-player key with a TTL, so stale entries expire on their own.
 *
 * @property server display name of the server the player is on
 * @property uuid the player's UUID
 * @property name the player's name
 * @property buffer the player's current AI suspicion buffer
 * @property ping the player's ping in milliseconds
 */
data class SuspiciousSnapshot
@JsonCreator
constructor(
  @param:JsonProperty("server") val server: String,
  @param:JsonProperty("uuid") val uuid: String,
  @param:JsonProperty("name") val name: String,
  @param:JsonProperty("buffer") val buffer: Double,
  @param:JsonProperty("ping") val ping: Int,
)
