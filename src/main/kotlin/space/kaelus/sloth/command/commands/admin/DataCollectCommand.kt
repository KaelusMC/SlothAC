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

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.parser.PlayerParser
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.description.Description
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.kotlin.extension.suggestionProvider
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionProvider
import space.kaelus.sloth.checks.impl.ai.DataCollectorManager
import space.kaelus.sloth.command.SlothCommand
import space.kaelus.sloth.data.DataSession
import space.kaelus.sloth.sender.Sender
import space.kaelus.sloth.utils.Message
import space.kaelus.sloth.utils.MessageUtil

class DataCollectCommand(private val dataCollectorManager: DataCollectorManager) : SlothCommand {
  override fun register(manager: CommandManager<Sender>) {
    val typeSuggestions = listOf("LEGIT", "CHEAT", "UNLABELED").map { Suggestion.suggestion(it) }

    val typeProvider = SuggestionProvider.suggesting<Sender>(typeSuggestions)

    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("datacollect", Description.empty(), "dc")
        .permission("sloth.datacollect")
        .literal("start")
        .required("target", PlayerParser.playerParser())
        .required("type", StringParser.stringParser()) { suggestionProvider = typeProvider }
        .optional("details", StringParser.greedyStringParser())
        .handler(this@DataCollectCommand::start)
    }

    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("datacollect", Description.empty(), "dc")
        .permission("sloth.datacollect")
        .literal("stop")
        .required("target", PlayerParser.playerParser())
        .handler(this@DataCollectCommand::stop)
    }

    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("datacollect", Description.empty(), "dc")
        .permission("sloth.datacollect")
        .literal("status")
        .optional("target", PlayerParser.playerParser())
        .handler(this@DataCollectCommand::status)
    }

    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("datacollect", Description.empty(), "dc")
        .permission("sloth.datacollect")
        .literal("global")
        .literal("start")
        .required("type", StringParser.stringParser()) { suggestionProvider = typeProvider }
        .optional("details", StringParser.greedyStringParser())
        .handler(this@DataCollectCommand::onGlobalStart)
    }

    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("datacollect", Description.empty(), "dc")
        .permission("sloth.datacollect")
        .literal("global")
        .literal("stop")
        .handler(this@DataCollectCommand::onGlobalStop)
    }

    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("datacollect", Description.empty(), "dc")
        .permission("sloth.datacollect")
        .literal("global")
        .literal("status")
        .handler(this@DataCollectCommand::onGlobalStatus)
    }
  }

  private fun start(context: CommandContext<Sender>) {
    val sender: CommandSender = context.sender().nativeSender
    val target: Player = context["target"]
    val type = context.get<String>("type").uppercase(Locale.ROOT)
    val details: String = context.getOrDefault("details", "")

    val statusDetails = resolveStatus(type, details, sender) ?: return

    if (dataCollectorManager.startCollecting(target.uniqueId, target.name, statusDetails)) {
      MessageUtil.sendMessage(
        sender,
        Message.DATACOLLECT_START_SUCCESS,
        "player",
        target.name,
        "status",
        statusDetails,
      )
    } else {
      MessageUtil.sendMessage(sender, Message.DATACOLLECT_START_RESTARTED, "player", target.name)
    }
  }

  private fun stop(context: CommandContext<Sender>) {
    val sender: CommandSender = context.sender().nativeSender
    val target: Player = context["target"]

    if (dataCollectorManager.stopCollecting(target.uniqueId)) {
      MessageUtil.sendMessage(sender, Message.DATACOLLECT_STOP_SUCCESS, "player", target.name)
    } else {
      MessageUtil.sendMessage(sender, Message.DATACOLLECT_STOP_FAIL, "player", target.name)
    }
  }

  private fun status(context: CommandContext<Sender>) {
    val sender: CommandSender = context.sender().nativeSender
    val target: Player? = context.getOrDefault("target", null)

    if (target != null) {
      val session = dataCollectorManager.getSession(target.uniqueId)
      if (session != null) {
        val seconds = Duration.between(session.startTime, Instant.now()).toSeconds()
        MessageUtil.sendMessage(
          sender,
          Message.DATACOLLECT_STATUS_PLAYER,
          "player",
          target.name,
          "status",
          session.status,
          "time",
          seconds.toString(),
          "ticks",
          session.recordedTicks.size.toString(),
        )
      } else {
        MessageUtil.sendMessage(
          sender,
          Message.DATACOLLECT_STATUS_NO_SESSION,
          "player",
          target.name,
        )
      }
      return
    }

    MessageUtil.sendMessage(sender, Message.DATACOLLECT_STATUS_HEADER)
    if (dataCollectorManager.activeSessions.isEmpty()) {
      MessageUtil.sendMessage(sender, Message.DATACOLLECT_STATUS_NONE)
      return
    }

    for (session: DataSession in dataCollectorManager.activeSessions.values) {
      val seconds = Duration.between(session.startTime, Instant.now()).toSeconds()
      MessageUtil.sendMessage(
        sender,
        Message.DATACOLLECT_STATUS_PLAYER,
        "player",
        session.player,
        "status",
        session.status,
        "time",
        seconds.toString(),
        "ticks",
        session.recordedTicks.size.toString(),
      )
    }
  }

  private fun onGlobalStart(context: CommandContext<Sender>) {
    val sender: CommandSender = context.sender().nativeSender
    val type = context.get<String>("type").uppercase(Locale.ROOT)
    val details: String = context.getOrDefault("details", "")

    val status = resolveStatus(type, details, sender) ?: return

    val oldGlobalId = dataCollectorManager.globalCollectionId
    if (oldGlobalId != null) {
      MessageUtil.sendMessage(
        sender,
        Message.DATACOLLECT_GLOBAL_STOP_PREVIOUS,
        "id",
        oldGlobalId.replace('#', ' '),
      )
      dataCollectorManager.stopGlobalCollection()
    }

    val newGlobalId =
      String.format(
        "%s_GLOBAL_%s",
        status.replace(' ', '#'),
        TIMESTAMP_FORMAT.format(Instant.now()),
      )
    dataCollectorManager.globalCollectionId = newGlobalId
    MessageUtil.sendMessage(
      sender,
      Message.DATACOLLECT_GLOBAL_START_SUCCESS,
      "id",
      newGlobalId.replace('#', ' '),
    )

    var startedCount = 0
    for (onlinePlayer in Bukkit.getOnlinePlayers()) {
      if (
        dataCollectorManager.startCollecting(onlinePlayer.uniqueId, onlinePlayer.name, newGlobalId)
      ) {
        startedCount++
      }
    }
    if (startedCount > 0) {
      MessageUtil.sendMessage(
        sender,
        Message.DATACOLLECT_GLOBAL_STARTED_FOR_PLAYERS,
        "count",
        startedCount.toString(),
      )
    }
  }

  private fun onGlobalStop(context: CommandContext<Sender>) {
    val sender: CommandSender = context.sender().nativeSender
    val oldGlobalId = dataCollectorManager.globalCollectionId
    if (oldGlobalId == null) {
      MessageUtil.sendMessage(sender, Message.DATACOLLECT_GLOBAL_STOP_FAIL)
      return
    }
    val stoppedCount = dataCollectorManager.stopGlobalCollection()
    MessageUtil.sendMessage(
      sender,
      Message.DATACOLLECT_GLOBAL_STOP_SUCCESS,
      "id",
      oldGlobalId.replace('#', ' '),
    )
    if (stoppedCount > 0) {
      MessageUtil.sendMessage(
        sender,
        Message.DATACOLLECT_GLOBAL_STOP_ARCHIVED,
        "count",
        stoppedCount.toString(),
      )
    }
  }

  private fun onGlobalStatus(context: CommandContext<Sender>) {
    val sender: CommandSender = context.sender().nativeSender
    val globalId = dataCollectorManager.globalCollectionId
    if (globalId == null) {
      MessageUtil.sendMessage(sender, Message.DATACOLLECT_GLOBAL_STATUS_FAIL)
      return
    }
    MessageUtil.sendMessage(sender, Message.DATACOLLECT_GLOBAL_STATUS_HEADER)
    MessageUtil.sendMessage(sender, Message.DATACOLLECT_GLOBAL_STATUS_ACTIVE)
    MessageUtil.sendMessage(
      sender,
      Message.DATACOLLECT_GLOBAL_STATUS_SESSION_ID,
      "id",
      globalId.replace('#', ' '),
    )
    MessageUtil.sendMessage(sender, Message.DATACOLLECT_GLOBAL_STATUS_PLAYERS_HEADER)

    var playerCount = 0
    for (session in dataCollectorManager.activeSessions.values) {
      if (globalId == session.status) {
        val seconds = Duration.between(session.startTime, Instant.now()).toSeconds()
        MessageUtil.sendMessage(
          sender,
          Message.DATACOLLECT_GLOBAL_STATUS_PLAYER_ENTRY,
          "player",
          session.player,
          "time",
          seconds.toString(),
          "ticks",
          session.recordedTicks.size.toString(),
        )
        playerCount++
      }
    }
    if (playerCount == 0) {
      MessageUtil.sendMessage(sender, Message.DATACOLLECT_STATUS_NONE)
    }
  }

  private fun resolveStatus(type: String, details: String, sender: CommandSender): String? {
    return when (type) {
      "LEGIT",
      "CHEAT" -> {
        if (details.isEmpty()) {
          MessageUtil.sendMessage(sender, Message.DATACOLLECT_DETAILS_REQUIRED)
          null
        } else {
          "$type $details"
        }
      }
      "UNLABELED" -> "UNLABELED"
      else -> {
        MessageUtil.sendMessage(sender, Message.DATACOLLECT_INVALID_TYPE)
        null
      }
    }
  }

  private companion object {
    private val TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault())
  }
}
