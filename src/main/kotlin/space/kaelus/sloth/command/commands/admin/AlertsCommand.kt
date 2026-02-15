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
package space.kaelus.sloth.command.commands.admin

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.kotlin.extension.buildAndRegister
import space.kaelus.sloth.alert.AlertManager
import space.kaelus.sloth.alert.AlertType
import space.kaelus.sloth.command.SlothCommand
import space.kaelus.sloth.sender.Sender
import space.kaelus.sloth.utils.Message
import space.kaelus.sloth.utils.MessageUtil

class AlertsCommand(private val alertManager: AlertManager) : SlothCommand {
  override fun register(manager: CommandManager<Sender>) {
    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("alerts").permission("sloth.alerts").handler(this@AlertsCommand::execute)
    }
  }

  private fun execute(context: CommandContext<Sender>) {
    val nativeSender: CommandSender = context.sender().nativeSender

    if (nativeSender is Player) {
      alertManager.toggle(nativeSender, AlertType.REGULAR, false)
    } else {
      alertManager.toggleConsoleAlerts(AlertType.REGULAR)
      if (alertManager.isConsoleAlertsEnabled(AlertType.REGULAR)) {
        MessageUtil.sendMessage(nativeSender, Message.ALERTS_ENABLED)
      } else {
        MessageUtil.sendMessage(nativeSender, Message.ALERTS_DISABLED)
      }
    }
  }
}
