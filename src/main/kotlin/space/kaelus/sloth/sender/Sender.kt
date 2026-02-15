/*
 * This file is part of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2026 GrimAC, DefineOutside and contributors
 *
 * GrimAC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GrimAC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package space.kaelus.sloth.sender

import java.util.UUID
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

interface Sender {
  companion object {
    @JvmField val CONSOLE_UUID: UUID = UUID(0L, 0L)
    @JvmField val CONSOLE_NAME: String = "Console"
  }

  val name: String

  val uniqueId: UUID

  fun sendMessage(message: String)

  fun sendMessage(message: Component)

  fun hasPermission(permission: String): Boolean

  val isConsole: Boolean

  val isPlayer: Boolean

  val nativeSender: CommandSender

  val player: Player?
}
