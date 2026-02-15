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

import java.util.logging.Logger
import org.bukkit.Statistic
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.CommandManager
import org.incendo.cloud.bukkit.parser.PlayerParser
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.kotlin.extension.buildAndRegister
import space.kaelus.sloth.checks.impl.ai.AiCheck
import space.kaelus.sloth.checks.impl.combat.AimProcessor
import space.kaelus.sloth.command.SlothCommand
import space.kaelus.sloth.config.LocaleManager
import space.kaelus.sloth.player.PlayerDataManager
import space.kaelus.sloth.player.SlothPlayer
import space.kaelus.sloth.sender.Sender
import space.kaelus.sloth.utils.Message
import space.kaelus.sloth.utils.MessageUtil
import space.kaelus.sloth.utils.TimeUtil

class ProfileCommand(
  private val playerDataManager: PlayerDataManager,
  private val localeManager: LocaleManager,
  private val logger: Logger,
) : SlothCommand {
  override fun register(manager: CommandManager<Sender>) {
    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("profile")
        .permission("sloth.profile")
        .required("target", PlayerParser.playerParser())
        .handler(this@ProfileCommand::execute)
    }
  }

  private fun execute(context: CommandContext<Sender>) {
    val sender: CommandSender = context.sender().nativeSender
    val target: Player = context["target"]

    val slothPlayer: SlothPlayer =
      playerDataManager.getPlayer(target)
        ?: run {
          MessageUtil.sendMessage(sender, Message.PROFILE_NO_DATA)
          return
        }

    val aiCheck = slothPlayer.checkManager.getCheck(AiCheck::class.java)
    val aimProcessor = slothPlayer.checkManager.getCheck(AimProcessor::class.java)

    val sessionMillis = System.currentTimeMillis() - slothPlayer.joinTime
    var totalPlayTicks = 0L
    try {
      totalPlayTicks = target.getStatistic(Statistic.PLAY_ONE_MINUTE).toLong()
    } catch (_: IllegalArgumentException) {
      logger.fine("Failed to fetch PLAY_ONE_MINUTE for ${target.name}")
    }

    val totalPlayMillis = totalPlayTicks * 50

    MessageUtil.sendMessageList(
      sender,
      Message.PROFILE_LINES,
      "player",
      target.name,
      "ping",
      target.ping.toString(),
      "version",
      slothPlayer.user.clientVersion.releaseName,
      "brand",
      slothPlayer.brand,
      "session_time",
      TimeUtil.formatDuration(sessionMillis, localeManager),
      "total_playtime",
      TimeUtil.formatDuration(totalPlayMillis, localeManager),
      "sens_x",
      if (aimProcessor != null) String.format("%.2f", aimProcessor.sensitivityX * 200) else "N/A",
      "sens_y",
      if (aimProcessor != null) String.format("%.2f", aimProcessor.sensitivityY * 200) else "N/A",
      "ai_buffer",
      if (aiCheck != null) String.format("%.2f", aiCheck.buffer) else "N/A",
      "ai_probs_90",
      if (aiCheck != null) aiCheck.prob90.toString() else "N/A",
    )
  }
}
