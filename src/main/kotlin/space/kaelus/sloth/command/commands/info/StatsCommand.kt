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
import org.bukkit.Bukkit
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.kotlin.extension.buildAndRegister
import space.kaelus.sloth.checks.impl.ai.AiCheck
import space.kaelus.sloth.command.SlothCommand
import space.kaelus.sloth.database.DatabaseManager
import space.kaelus.sloth.database.ViolationDatabase
import space.kaelus.sloth.player.PlayerDataManager
import space.kaelus.sloth.scheduler.SchedulerService
import space.kaelus.sloth.sender.Sender
import space.kaelus.sloth.utils.Message
import space.kaelus.sloth.utils.MessageUtil

class StatsCommand(
  private val databaseManager: DatabaseManager,
  private val scheduler: SchedulerService,
  private val playerDataManager: PlayerDataManager,
) : SlothCommand {
  override fun register(manager: CommandManager<Sender>) {
    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("stats").permission("sloth.stats").handler(this@StatsCommand::execute)
    }
  }

  private fun execute(context: CommandContext<Sender>) {
    val sender = context.sender()
    val db: ViolationDatabase = databaseManager.database

    scheduler.runAsync {
      val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)

      val totalFlags = db.getLogCount(since)
      val uniqueViolators = db.getUniqueViolatorsSince(since)

      scheduler.runSync {
        MessageUtil.sendMessageList(
          sender.nativeSender,
          Message.STATS_LINES,
          "flags_24h",
          totalFlags.toString(),
          "violators_24h",
          uniqueViolators.toString(),
          "online_players",
          Bukkit.getOnlinePlayers().size.toString(),
          "suspicious_now",
          getSuspiciousCount().toString(),
        )
      }
    }
  }

  private fun getSuspiciousCount(): Long {
    return playerDataManager
      .getPlayers()
      .asSequence()
      .filter { sp ->
        val check = sp.checkManager.getCheck(AiCheck::class.java)
        check != null && check.buffer > 10
      }
      .count()
      .toLong()
  }
}
