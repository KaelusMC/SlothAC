/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2025 KaelusMC
 *
 * This file contains code derived from GrimAC.
 * The original authors of GrimAC are credited below.
 *
 * Copyright (c) 2021-2025 GrimAC, DefineOutside and contributors.
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
package space.kaelus.sloth.command.commands;

import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.context.CommandContext;
import space.kaelus.sloth.checks.impl.ai.AICheck;
import space.kaelus.sloth.checks.impl.aim.AimProcessor;
import space.kaelus.sloth.command.SlothCommand;
import space.kaelus.sloth.config.LocaleManager;
import space.kaelus.sloth.player.PlayerDataManager;
import space.kaelus.sloth.player.SlothPlayer;
import space.kaelus.sloth.sender.Sender;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;
import space.kaelus.sloth.utils.TimeUtil;

public class ProfileCommand implements SlothCommand {
  private final PlayerDataManager playerDataManager;
  private final LocaleManager localeManager;

  public ProfileCommand(PlayerDataManager playerDataManager, LocaleManager localeManager) {
    this.playerDataManager = playerDataManager;
    this.localeManager = localeManager;
  }

  @Override
  public void register(CommandManager<Sender> manager) {
    manager.command(
        manager
            .commandBuilder("sloth", "slothac")
            .literal("profile")
            .permission("sloth.profile")
            .required("target", PlayerParser.playerParser())
            .handler(this::execute));
  }

  private void execute(CommandContext<Sender> context) {
    final CommandSender sender = context.sender().getNativeSender();
    final Player target = context.get("target");

    SlothPlayer slothPlayer = playerDataManager.getPlayer(target);
    if (slothPlayer == null) {
      MessageUtil.sendMessage(sender, Message.PROFILE_NO_DATA);
      return;
    }

    AICheck aiCheck = slothPlayer.getCheckManager().getCheck(AICheck.class);
    AimProcessor aimProcessor = slothPlayer.getCheckManager().getCheck(AimProcessor.class);

    long sessionMillis = System.currentTimeMillis() - slothPlayer.getJoinTime();
    long totalPlayTicks = 0;
    try {
      totalPlayTicks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
    } catch (IllegalArgumentException ignored) {
    }

    long totalPlayMillis = totalPlayTicks * 50;

    MessageUtil.sendMessageList(
        sender,
        Message.PROFILE_LINES,
        "player",
        target.getName(),
        "ping",
        String.valueOf(target.getPing()),
        "version",
        slothPlayer.getUser().getClientVersion().getReleaseName(),
        "brand",
        slothPlayer.getBrand(),
        "session_time",
        TimeUtil.formatDuration(sessionMillis, localeManager),
        "total_playtime",
        TimeUtil.formatDuration(totalPlayMillis, localeManager),
        "sens_x",
        (aimProcessor != null)
            ? String.format("%.2f", aimProcessor.getSensitivityX() * 200)
            : "N/A",
        "sens_y",
        (aimProcessor != null)
            ? String.format("%.2f", aimProcessor.getSensitivityY() * 200)
            : "N/A",
        "ai_buffer",
        (aiCheck != null) ? String.format("%.2f", aiCheck.getBuffer()) : "N/A",
        "ai_probs_90",
        (aiCheck != null) ? String.valueOf(aiCheck.getProb90()) : "N/A");
  }
}
