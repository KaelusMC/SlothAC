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

import java.util.Locale
import java.util.concurrent.TimeUnit
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
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

private const val SUSPICIOUS_BUFFER_THRESHOLD = 10.0
private const val PERCENT_MULTIPLIER = 100.0
private const val WHOLE_PERCENT_DISPLAY_THRESHOLD = 10.0
private const val WHOLE_NUMBER_REMAINDER = 1.0

private fun hasBeenSeenSince(player: OfflinePlayer, since: Long): Boolean {
  return player.hasPlayedBefore() && player.lastSeen >= since
}

private fun formatThreshold(value: Double): String {
  return if (value % WHOLE_NUMBER_REMAINDER == 0.0) {
    value.toInt().toString()
  } else {
    String.format(Locale.US, "%.1f", value)
  }
}

class StatsCommand(
  private val databaseManager: DatabaseManager,
  private val scheduler: SchedulerService,
  private val playerDataManager: PlayerDataManager,
) : SlothCommand {
  private data class StatsSnapshot(
    val totalFlags: Int,
    val uniquePlayers24h: Int,
    val uniqueViolators: Int,
    val violatorPercent: String,
    val onlinePlayers: Int,
    val suspiciousNow: Long,
    val suspiciousPercent: String,
  )

  override fun register(manager: CommandManager<Sender>) {
    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("stats").permission("sloth.stats").handler(this@StatsCommand::execute)
    }
  }

  private fun execute(context: CommandContext<Sender>) {
    val sender = context.sender()
    val db: ViolationDatabase = databaseManager.database
    val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
    val uniquePlayers24h = countUniquePlayersSince(since)
    val onlinePlayers = Bukkit.getOnlinePlayers().size
    val suspiciousNow = getSuspiciousCount()

    if (!databaseManager.isAvailable) {
      MessageUtil.sendMessage(sender.nativeSender, Message.STORAGE_DEGRADED)
    }

    scheduler.runAsync {
      val totalFlags = db.getLogCount(since)
      val uniqueViolators = db.getUniqueViolatorsSince(since)
      val snapshot =
        StatsSnapshot(
          totalFlags = totalFlags,
          uniquePlayers24h = uniquePlayers24h,
          uniqueViolators = uniqueViolators,
          violatorPercent = formatPercent(uniqueViolators, uniquePlayers24h),
          onlinePlayers = onlinePlayers,
          suspiciousNow = suspiciousNow,
          suspiciousPercent = formatPercent(suspiciousNow, onlinePlayers),
        )

      scheduler.runSync { buildStatsLines(snapshot).forEach(sender::sendMessage) }
    }
  }

  private fun buildStatsLines(snapshot: StatsSnapshot): List<Component> {
    return listOf(
      MessageUtil.getMessage(Message.STATS_HEADER),
      buildFlagsLine(snapshot),
      buildPlayersLine(snapshot),
      buildViolatorsLine(snapshot),
      MessageUtil.getMessage(
        Message.STATS_ONLINE,
        "online_players",
        snapshot.onlinePlayers.toString(),
      ),
      buildSuspiciousLine(snapshot),
    )
  }

  private fun buildFlagsLine(snapshot: StatsSnapshot): Component =
    MessageUtil.getMessage(Message.STATS_FLAGS, "flags_24h", snapshot.totalFlags.toString())
      .hoverEvent(HoverEvent.showText(MessageUtil.getMessage(Message.STATS_FLAGS_HOVER)))
      .clickEvent(ClickEvent.runCommand("/sloth logs"))

  private fun buildPlayersLine(snapshot: StatsSnapshot): Component =
    MessageUtil.getMessage(
        Message.STATS_PLAYERS,
        "players_24h",
        snapshot.uniquePlayers24h.toString(),
      )
      .hoverEvent(
        HoverEvent.showText(
          MessageUtil.getMessage(
            Message.STATS_PLAYERS_HOVER,
            "players_24h",
            snapshot.uniquePlayers24h.toString(),
          )
        )
      )

  private fun buildViolatorsLine(snapshot: StatsSnapshot): Component =
    MessageUtil.getMessage(
        Message.STATS_VIOLATORS,
        "violators_24h",
        snapshot.uniqueViolators.toString(),
        "violators_percent_24h",
        snapshot.violatorPercent,
      )
      .hoverEvent(
        HoverEvent.showText(
          MessageUtil.getMessage(
            Message.STATS_VIOLATORS_HOVER,
            "violators_percent_24h",
            snapshot.violatorPercent,
          )
        )
      )

  private fun buildSuspiciousLine(snapshot: StatsSnapshot): Component =
    MessageUtil.getMessage(
        Message.STATS_SUSPICIOUS,
        "suspicious_now",
        snapshot.suspiciousNow.toString(),
        "suspicious_percent_now",
        snapshot.suspiciousPercent,
      )
      .hoverEvent(
        HoverEvent.showText(
          MessageUtil.getMessage(
            Message.STATS_SUSPICIOUS_HOVER,
            "suspicious_now",
            snapshot.suspiciousNow.toString(),
            "online_players",
            snapshot.onlinePlayers.toString(),
            "suspicious_threshold",
            formatThreshold(SUSPICIOUS_BUFFER_THRESHOLD),
          )
        )
      )
      .clickEvent(ClickEvent.runCommand("/sloth suspicious list"))

  private fun countUniquePlayersSince(since: Long): Int {
    return Bukkit.getOfflinePlayers().count { player -> hasBeenSeenSince(player, since) }
  }

  private fun getSuspiciousCount(): Long {
    return playerDataManager
      .getPlayers()
      .asSequence()
      .filter { sp ->
        val check = sp.checkManager.getCheck(AiCheck::class.java)
        check != null && check.buffer > SUSPICIOUS_BUFFER_THRESHOLD
      }
      .count()
      .toLong()
  }

  private fun formatPercent(numerator: Number, denominator: Int): String {
    if (denominator <= 0) {
      return "0%"
    }

    val percent = numerator.toDouble() / denominator.toDouble() * PERCENT_MULTIPLIER
    return if (
      percent >= WHOLE_PERCENT_DISPLAY_THRESHOLD || percent % WHOLE_NUMBER_REMAINDER == 0.0
    ) {
      String.format(Locale.US, "%.0f%%", percent)
    } else {
      String.format(Locale.US, "%.1f%%", percent)
    }
  }
}
