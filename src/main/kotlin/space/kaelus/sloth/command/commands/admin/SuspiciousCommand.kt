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
package space.kaelus.sloth.command.commands.admin

import java.util.Locale
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.kotlin.extension.buildAndRegister
import space.kaelus.sloth.alert.AlertManager
import space.kaelus.sloth.alert.AlertType
import space.kaelus.sloth.checks.impl.ai.AiCheck
import space.kaelus.sloth.command.CommandRegister
import space.kaelus.sloth.command.SlothCommand
import space.kaelus.sloth.command.requirements.PlayerSenderRequirement
import space.kaelus.sloth.database.DatabaseManager
import space.kaelus.sloth.player.PlayerDataManager
import space.kaelus.sloth.player.SlothPlayer
import space.kaelus.sloth.scheduler.SchedulerService
import space.kaelus.sloth.sender.Sender
import space.kaelus.sloth.utils.Message
import space.kaelus.sloth.utils.MessageUtil

class SuspiciousCommand(
  private val playerDataManager: PlayerDataManager,
  private val alertManager: AlertManager,
  private val databaseManager: DatabaseManager,
  private val scheduler: SchedulerService,
) : SlothCommand {
  private data class SuspiciousEntry(val player: SlothPlayer, val check: AiCheck)

  private data class FlaggedPlayerEntry(val playerName: String, val flags: Int)

  override fun register(manager: CommandManager<Sender>) {
    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("suspicious")
        .permission("sloth.suspicious")
        .literal("alerts")
        .permission("sloth.suspicious.alerts")
        .mutate { it.apply(CommandRegister.REQUIREMENT_FACTORY.create(PlayerSenderRequirement)) }
        .handler(this@SuspiciousCommand::executeAlerts)
    }

    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("suspicious")
        .permission("sloth.suspicious")
        .literal("list")
        .permission("sloth.suspicious.list")
        .handler(this@SuspiciousCommand::executeList)
    }

    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("suspicious")
        .permission("sloth.suspicious")
        .literal("top")
        .permission("sloth.suspicious.top")
        .handler(this@SuspiciousCommand::executeTop)
    }

    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("suspicious")
        .permission("sloth.suspicious")
        .literal("flagged")
        .permission("sloth.suspicious.flagged")
        .handler(this@SuspiciousCommand::executeFlagged)
    }
  }

  private fun executeAlerts(context: CommandContext<Sender>) {
    val player = context.sender().player ?: return
    alertManager.toggle(player, AlertType.SUSPICIOUS, false)
  }

  private fun executeList(context: CommandContext<Sender>) {
    val sender = context.sender()
    val bufferFilter = 0.0

    val suspiciousPlayers = ArrayList<SuspiciousEntry>()
    for (sp in playerDataManager.getPlayers()) {
      val check = sp.checkManager.getCheck(AiCheck::class.java) ?: continue
      if (check.buffer > bufferFilter) {
        suspiciousPlayers.add(SuspiciousEntry(sp, check))
      }
    }

    suspiciousPlayers.sortByDescending { it.check.buffer }

    if (suspiciousPlayers.isEmpty()) {
      sender.sendMessage(MessageUtil.getMessage(Message.SUSPICIOUS_LIST_EMPTY))
      return
    }

    sender.sendMessage(
      MessageUtil.getMessage(
        Message.SUSPICIOUS_LIST_HEADER,
        "count",
        suspiciousPlayers.size.toString(),
      )
    )

    for (item in suspiciousPlayers) {
      val buffer = item.check.buffer
      val playerName = item.player.player.name

      val message: Component =
        MessageUtil.getMessage(
            Message.SUSPICIOUS_LIST_ENTRY,
            "player",
            playerName,
            "buffer",
            String.format(Locale.US, "%.1f", buffer),
            "ping",
            item.player.player.ping.toString(),
          )
          .hoverEvent(
            HoverEvent.showText(MessageUtil.getMessage(Message.SUSPICIOUS_LIST_ENTRY_HOVER))
          )
          .clickEvent(ClickEvent.runCommand("/sloth profile $playerName"))

      sender.sendMessage(message)
    }
  }

  private fun executeTop(context: CommandContext<Sender>) {
    val sender = context.sender()

    var topPlayer: SlothPlayer? = null
    var topBuffer = 0.0

    for (sp in playerDataManager.getPlayers()) {
      val check = sp.checkManager.getCheck(AiCheck::class.java) ?: continue
      val buffer = check.buffer
      if (buffer > topBuffer) {
        topBuffer = buffer
        topPlayer = sp
      }
    }

    if (topPlayer == null || topBuffer == 0.0) {
      sender.sendMessage(MessageUtil.getMessage(Message.SUSPICIOUS_TOP_NONE))
      return
    }

    val playerName = topPlayer.player.name

    val message: Component =
      MessageUtil.getMessage(
          Message.SUSPICIOUS_TOP_PLAYER,
          "player",
          playerName,
          "buffer",
          String.format(Locale.US, "%.1f", topBuffer),
        )
        .hoverEvent(
          HoverEvent.showText(MessageUtil.getMessage(Message.SUSPICIOUS_TOP_PLAYER_HOVER))
        )
        .clickEvent(ClickEvent.runCommand("/sloth monitor $playerName"))

    sender.sendMessage(message)
  }

  private fun executeFlagged(context: CommandContext<Sender>) {
    val sender = context.sender()
    val onlinePlayers =
      Bukkit.getOnlinePlayers()
        .map { player -> player.uniqueId to player.name }
        .toMap(LinkedHashMap())

    if (onlinePlayers.isEmpty()) {
      sender.sendMessage(MessageUtil.getMessage(Message.SUSPICIOUS_FLAGGED_EMPTY))
      return
    }

    scheduler.runAsync {
      val flagCounts = databaseManager.database.getLogCounts(onlinePlayers.keys)
      val flaggedPlayers =
        flagCounts.entries
          .asSequence()
          .filter { it.value > 0 }
          .mapNotNull { (uuid, flags) ->
            onlinePlayers[uuid]?.let { playerName -> FlaggedPlayerEntry(playerName, flags) }
          }
          .sortedWith(compareByDescending<FlaggedPlayerEntry> { it.flags }.thenBy { it.playerName })
          .toList()

      scheduler.runSync {
        if (flaggedPlayers.isEmpty()) {
          sender.sendMessage(MessageUtil.getMessage(Message.SUSPICIOUS_FLAGGED_EMPTY))
          return@runSync
        }

        sender.sendMessage(
          MessageUtil.getMessage(
            Message.SUSPICIOUS_FLAGGED_HEADER,
            "count",
            flaggedPlayers.size.toString(),
          )
        )

        for (entry in flaggedPlayers) {
          sender.sendMessage(
            MessageUtil.getMessage(
                Message.SUSPICIOUS_FLAGGED_ENTRY,
                "player",
                entry.playerName,
                "flags",
                entry.flags.toString(),
              )
              .hoverEvent(
                HoverEvent.showText(MessageUtil.getMessage(Message.SUSPICIOUS_LIST_ENTRY_HOVER))
              )
              .clickEvent(ClickEvent.runCommand("/sloth profile ${entry.playerName}"))
          )
        }
      }
    }
  }
}
