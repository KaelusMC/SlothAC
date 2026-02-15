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
package space.kaelus.sloth.command.commands.info

import org.bukkit.OfflinePlayer
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.parser.OfflinePlayerParser
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.description.Description
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.parser.standard.IntegerParser
import space.kaelus.sloth.command.SlothCommand
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.config.LocaleManager
import space.kaelus.sloth.database.DatabaseManager
import space.kaelus.sloth.database.Violation
import space.kaelus.sloth.scheduler.SchedulerService
import space.kaelus.sloth.sender.Sender
import space.kaelus.sloth.utils.Message
import space.kaelus.sloth.utils.MessageUtil
import space.kaelus.sloth.utils.TimeUtil

class HistoryCommand(
  private val databaseManager: DatabaseManager,
  private val configManager: ConfigManager,
  private val localeManager: LocaleManager,
  private val scheduler: SchedulerService,
) : SlothCommand {
  override fun register(manager: CommandManager<Sender>) {
    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("history", Description.empty(), "hist")
        .permission("sloth.history")
        .required("target", OfflinePlayerParser.offlinePlayerParser())
        .optional("page", IntegerParser.integerParser(1))
        .handler(this@HistoryCommand::handleHistory)
    }
  }

  private fun handleHistory(context: CommandContext<Sender>) {
    val sender = context.sender()
    val target: OfflinePlayer = context["target"]
    val page: Int = context.getOrDefault("page", 1)

    if (!configManager.config.getBoolean("history.enabled", false)) {
      MessageUtil.sendMessage(sender.nativeSender, Message.HISTORY_DISABLED)
      return
    }

    if (!target.hasPlayedBefore() && !target.isOnline) {
      MessageUtil.sendMessage(sender.nativeSender, Message.PLAYER_NOT_FOUND)
      return
    }

    val targetName = target.name ?: target.uniqueId.toString()
    val targetId = target.uniqueId

    scheduler.runAsync {
      val entriesPerPage = 10
      val violations: List<Violation> =
        databaseManager.database.getViolations(targetId, page, entriesPerPage)
      val totalLogs = databaseManager.database.getLogCount(targetId)
      val maxPages =
        kotlin.math.max(1, kotlin.math.ceil(totalLogs.toDouble() / entriesPerPage).toInt())

      val header =
        MessageUtil.getMessage(
          Message.HISTORY_HEADER,
          "player",
          targetName,
          "page",
          page.toString(),
          "max_pages",
          maxPages.toString(),
        )

      val entries =
        violations.map { violation ->
          MessageUtil.getMessage(
            Message.HISTORY_ENTRY,
            "server",
            violation.serverName,
            "check",
            violation.checkName,
            "vl",
            violation.vl.toString(),
            "verbose",
            violation.verbose,
            "timeago",
            TimeUtil.formatTimeAgo(violation.createdAt, localeManager),
          )
        }

      scheduler.runSync {
        sender.sendMessage(header)

        if (entries.isEmpty()) {
          MessageUtil.sendMessage(sender.nativeSender, Message.HISTORY_NO_VIOLATIONS)
          return@runSync
        }

        for (entry in entries) {
          sender.sendMessage(entry)
        }
      }
    }
  }
}
