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
package space.kaelus.sloth.command.commands.info

import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.kotlin.extension.buildAndRegister
import space.kaelus.sloth.command.SlothCommand
import space.kaelus.sloth.sender.Sender
import space.kaelus.sloth.utils.Message
import space.kaelus.sloth.utils.MessageUtil

class HelpCommand : SlothCommand {
  override fun register(manager: CommandManager<Sender>) {
    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      permission("sloth.help")
      handler(this@HelpCommand::help)
    }
    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      permission("sloth.help")
      literal("help").handler(this@HelpCommand::help)
    }
  }

  private fun help(context: CommandContext<Sender>) {
    val sender = context.sender()
    MessageUtil.sendMessageList(sender.nativeSender, Message.HELP_MESSAGE, "command", "sloth")
  }
}
