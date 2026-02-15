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
package space.kaelus.sloth.api.event

import java.util.UUID

/**
 * Fired when a check flags a player.
 *
 * This event is dispatched on the calling thread.
 */
data class CheckFlagEvent(
  val playerId: UUID,
  val playerName: String,
  val checkName: String,
  val configName: String,
  val debug: String,
  override var cancelled: Boolean = false,
) : SlothCancellableEvent
