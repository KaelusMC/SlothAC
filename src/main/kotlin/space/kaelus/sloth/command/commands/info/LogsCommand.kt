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

import java.util.concurrent.TimeUnit
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
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

class LogsCommand(
  private val databaseManager: DatabaseManager,
  private val configManager: ConfigManager,
  private val localeManager: LocaleManager,
  private val scheduler: SchedulerService,
) : SlothCommand {
  override fun register(manager: CommandManager<Sender>) {
    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("logs")
        .permission("sloth.logs")
        .optional("page", IntegerParser.integerParser(1))
        .handler(this@LogsCommand::handleLogs)
    }
  }

  private fun handleLogs(context: CommandContext<Sender>) {
    val sender = context.sender()
    val page: Int = context.getOrDefault("page", 1)
    val timeArg: String? = null

    if (!configManager.config.getBoolean("history.enabled", false)) {
      MessageUtil.sendMessage(sender.nativeSender, Message.HISTORY_DISABLED)
      return
    }

    val since = parseTime(timeArg)
    if (since == -1L) {
      MessageUtil.sendMessage(sender.nativeSender, Message.LOGS_INVALID_TIME)
      return
    }

    scheduler.runAsync {
      val entriesPerPage = 10
      val violations: List<Violation> =
        databaseManager.database.getViolations(page, entriesPerPage, since)
      val totalLogs = databaseManager.database.getLogCount(since)
      val maxPages =
        kotlin.math.max(1, kotlin.math.ceil(totalLogs.toDouble() / entriesPerPage).toInt())

      val header =
        MessageUtil.getMessage(
          Message.LOGS_HEADER,
          "page",
          page.toString(),
          "max_pages",
          maxPages.toString(),
        )

      val entries =
        violations.map { violation ->
          MessageUtil.getMessage(
            Message.LOGS_ENTRY,
            "server",
            violation.serverName,
            "player",
            violation.playerName,
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
          MessageUtil.sendMessage(sender.nativeSender, Message.LOGS_NO_VIOLATIONS)
          return@runSync
        }

        for (entry in entries) {
          sender.sendMessage(entry)
        }
      }
    }
  }

  private fun parseTime(timeArg: String?): Long {
    if (timeArg == null) {
      return 0L
    }
    return try {
      if (timeArg.length < 2) return -1L
      val value = timeArg.substring(0, timeArg.length - 1).toLong()
      val unit = timeArg.last().lowercaseChar()
      val multiplier =
        when (unit) {
          'm' -> TimeUnit.MINUTES.toMillis(1)
          'h' -> TimeUnit.HOURS.toMillis(1)
          'd' -> TimeUnit.DAYS.toMillis(1)
          else -> null
        } ?: return -1L
      System.currentTimeMillis() - (value * multiplier)
    } catch (_: NumberFormatException) {
      -1L
    }
  }
}
