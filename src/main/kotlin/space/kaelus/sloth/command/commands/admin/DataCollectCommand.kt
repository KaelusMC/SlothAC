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
import java.util.Locale
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
    val typeSuggestions = listOf("LEGIT", "CHEAT").map { Suggestion.suggestion(it) }

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
      else -> {
        MessageUtil.sendMessage(sender, Message.DATACOLLECT_INVALID_TYPE)
        null
      }
    }
  }
}
