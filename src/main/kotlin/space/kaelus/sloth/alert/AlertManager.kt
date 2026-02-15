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
package space.kaelus.sloth.alert

import java.util.EnumMap
import java.util.EnumSet
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.config.LocaleManager
import space.kaelus.sloth.utils.Message
import space.kaelus.sloth.utils.MessageUtil

class AlertManager(
  private val configManager: ConfigManager,
  private val localeManager: LocaleManager,
  private val adventure: BukkitAudiences,
) {
  private val playersWithAlerts: MutableMap<AlertType, MutableSet<UUID>> =
    EnumMap(AlertType::class.java)
  private val consoleAlertsEnabled: MutableSet<AlertType> = EnumSet.allOf(AlertType::class.java)

  private var logToConsole = true

  var alertFormat: String = ""
    private set

  var brandAlertFormat: String = ""
    private set

  init {
    for (type in AlertType.values()) {
      playersWithAlerts[type] = CopyOnWriteArraySet()
    }
    reload()
  }

  fun reload() {
    logToConsole = configManager.config.getBoolean("alerts.print-to-console", true)
    alertFormat = localeManager.getRawMessage(Message.ALERTS_FORMAT)
    brandAlertFormat = localeManager.getRawMessage(Message.BRAND_NOTIFICATION)
  }

  fun toggle(player: Player, type: AlertType, silent: Boolean) {
    val playersSet = playersWithAlerts.getValue(type)
    val uuid = player.uniqueId

    if (playersSet.contains(uuid)) {
      playersSet.remove(uuid)
      if (!silent) {
        adventure(player).sendMessage(MessageUtil.getMessage(type.disabledMessage))
      }
    } else {
      playersSet.add(uuid)
      if (!silent) {
        adventure(player).sendMessage(MessageUtil.getMessage(type.enabledMessage))
      }
    }
  }

  fun send(component: Component, type: AlertType) {
    val playersSet = playersWithAlerts.getValue(type)
    val permission = type.permission

    for (uuid in playersSet) {
      val player = Bukkit.getPlayer(uuid)
      if (player != null && player.hasPermission(permission)) {
        adventure(player).sendMessage(component)
      }
    }

    if (logToConsole && consoleAlertsEnabled.contains(type)) {
      adventure(Bukkit.getConsoleSender()).sendMessage(component)
    }
  }

  fun hasAlertsEnabled(player: Player, type: AlertType): Boolean {
    return playersWithAlerts.getValue(type).contains(player.uniqueId)
  }

  fun isConsoleAlertsEnabled(type: AlertType): Boolean {
    return consoleAlertsEnabled.contains(type)
  }

  fun toggleConsoleAlerts(type: AlertType) {
    if (consoleAlertsEnabled.contains(type)) {
      consoleAlertsEnabled.remove(type)
    } else {
      consoleAlertsEnabled.add(type)
    }
  }

  fun handlePlayerQuit(player: Player) {
    val uuid = player.uniqueId
    for (players in playersWithAlerts.values) {
      players.remove(uuid)
    }
  }

  private fun adventure(player: Player): Audience {
    return adventure.player(player)
  }

  private fun adventure(sender: CommandSender): Audience {
    return adventure.sender(sender)
  }
}
