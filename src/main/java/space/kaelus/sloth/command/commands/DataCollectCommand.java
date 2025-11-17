/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2025 KaelusMC
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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import space.kaelus.sloth.checks.impl.ai.DataCollectorManager;
import space.kaelus.sloth.command.SlothCommand;
import space.kaelus.sloth.data.DataSession;
import space.kaelus.sloth.sender.Sender;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;

@Singleton
public class DataCollectCommand implements SlothCommand {

  private static final DateTimeFormatter TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

  private final DataCollectorManager dataCollectorManager;

  @Inject
  public DataCollectCommand(DataCollectorManager dataCollectorManager) {
    this.dataCollectorManager = dataCollectorManager;
  }

  @Override
  public void register(CommandManager<Sender> manager) {
    final var baseBuilder =
        manager
            .commandBuilder("sloth", "slothac")
            .literal("datacollect", "dc")
            .permission("sloth.datacollect");

    SuggestionProvider<Sender> typeProvider =
        SuggestionProvider.suggesting(
            List.of("LEGIT", "CHEAT", "UNLABELED").stream()
                .map(Suggestion::suggestion)
                .collect(Collectors.toList()));

    manager.command(
        baseBuilder
            .literal("start")
            .required("target", PlayerParser.playerParser())
            .required("type", StringParser.stringParser(), typeProvider)
            .optional("details", StringParser.greedyStringParser())
            .handler(this::start));

    manager.command(
        baseBuilder
            .literal("stop")
            .required("target", PlayerParser.playerParser())
            .handler(this::stop));

    manager.command(
        baseBuilder
            .literal("status")
            .optional("target", PlayerParser.playerParser())
            .handler(this::status));

    manager.command(
        baseBuilder
            .literal("global")
            .literal("start")
            .required("type", StringParser.stringParser(), typeProvider)
            .optional("details", StringParser.greedyStringParser())
            .handler(this::onGlobalStart));

    manager.command(baseBuilder.literal("global").literal("stop").handler(this::onGlobalStop));

    manager.command(baseBuilder.literal("global").literal("status").handler(this::onGlobalStatus));
  }

  private void start(CommandContext<Sender> context) {
    final CommandSender sender = context.sender().getNativeSender();
    final Player target = context.get("target");
    final String type = ((String) context.get("type")).toUpperCase(Locale.ROOT);
    final String details = context.getOrDefault("details", "");

    String statusDetails = resolveStatus(type, details, sender);

    if (statusDetails == null) {
      return;
    }

    if (dataCollectorManager.startCollecting(
        target.getUniqueId(), target.getName(), statusDetails)) {
      MessageUtil.sendMessage(
          sender,
          Message.DATACOLLECT_START_SUCCESS,
          "player",
          target.getName(),
          "status",
          statusDetails);
    } else {
      MessageUtil.sendMessage(
          sender, Message.DATACOLLECT_START_RESTARTED, "player", target.getName());
    }
  }

  private void stop(CommandContext<Sender> context) {
    final CommandSender sender = context.sender().getNativeSender();
    final Player target = context.get("target");

    if (dataCollectorManager.stopCollecting(target.getUniqueId())) {
      MessageUtil.sendMessage(sender, Message.DATACOLLECT_STOP_SUCCESS, "player", target.getName());
    } else {
      MessageUtil.sendMessage(sender, Message.DATACOLLECT_STOP_FAIL, "player", target.getName());
    }
  }

  private void status(CommandContext<Sender> context) {
    final CommandSender sender = context.sender().getNativeSender();
    final Player target = context.getOrDefault("target", null);

    if (target != null) {
      DataSession session = dataCollectorManager.getSession(target.getUniqueId());
      if (session != null) {
        long seconds = Duration.between(session.getStartTime(), Instant.now()).toSeconds();
        MessageUtil.sendMessage(
            sender,
            Message.DATACOLLECT_STATUS_PLAYER,
            "player",
            target.getName(),
            "status",
            session.getStatus(),
            "time",
            String.valueOf(seconds),
            "ticks",
            String.valueOf(session.getRecordedTicks().size()));
      } else {
        MessageUtil.sendMessage(
            sender, Message.DATACOLLECT_STATUS_NO_SESSION, "player", target.getName());
      }
    } else {
      MessageUtil.sendMessage(sender, Message.DATACOLLECT_STATUS_HEADER);
      if (dataCollectorManager.getActiveSessions().isEmpty()) {
        MessageUtil.sendMessage(sender, Message.DATACOLLECT_STATUS_NONE);
      } else {
        for (DataSession session : dataCollectorManager.getActiveSessions().values()) {
          long seconds = Duration.between(session.getStartTime(), Instant.now()).toSeconds();
          MessageUtil.sendMessage(
              sender,
              Message.DATACOLLECT_STATUS_PLAYER,
              "player",
              session.getPlayer(),
              "status",
              session.getStatus(),
              "time",
              String.valueOf(seconds),
              "ticks",
              String.valueOf(session.getRecordedTicks().size()));
        }
      }
    }
  }

  private void onGlobalStart(CommandContext<Sender> context) {
    final CommandSender sender = context.sender().getNativeSender();
    String type = ((String) context.get("type")).toUpperCase(Locale.ROOT);
    String details = context.getOrDefault("details", "");

    String status = resolveStatus(type, details, sender);

    if (status == null) {
      return;
    }

    String oldGlobalId = dataCollectorManager.getGlobalCollectionId();
    if (oldGlobalId != null) {
      MessageUtil.sendMessage(
          sender, Message.DATACOLLECT_GLOBAL_STOP_PREVIOUS, "id", oldGlobalId.replace('#', ' '));
      dataCollectorManager.stopGlobalCollection();
    }

    String newGlobalId =
        String.format(
            "%s_GLOBAL_%s", status.replace(' ', '#'), TIMESTAMP_FORMAT.format(Instant.now()));
    dataCollectorManager.setGlobalCollectionId(newGlobalId);
    MessageUtil.sendMessage(
        sender, Message.DATACOLLECT_GLOBAL_START_SUCCESS, "id", newGlobalId.replace('#', ' '));

    int startedCount = 0;
    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
      if (dataCollectorManager.startCollecting(
          onlinePlayer.getUniqueId(), onlinePlayer.getName(), newGlobalId)) {
        startedCount++;
      }
    }
    if (startedCount > 0) {
      MessageUtil.sendMessage(
          sender,
          Message.DATACOLLECT_GLOBAL_STARTED_FOR_PLAYERS,
          "count",
          String.valueOf(startedCount));
    }
  }

  private void onGlobalStop(CommandContext<Sender> context) {
    final CommandSender sender = context.sender().getNativeSender();
    String oldGlobalId = dataCollectorManager.getGlobalCollectionId();
    if (oldGlobalId == null) {
      MessageUtil.sendMessage(sender, Message.DATACOLLECT_GLOBAL_STOP_FAIL);
      return;
    }
    int stoppedCount = dataCollectorManager.stopGlobalCollection();
    MessageUtil.sendMessage(
        sender, Message.DATACOLLECT_GLOBAL_STOP_SUCCESS, "id", oldGlobalId.replace('#', ' '));
    if (stoppedCount > 0) {
      MessageUtil.sendMessage(
          sender, Message.DATACOLLECT_GLOBAL_STOP_ARCHIVED, "count", String.valueOf(stoppedCount));
    }
  }

  private void onGlobalStatus(CommandContext<Sender> context) {
    final CommandSender sender = context.sender().getNativeSender();
    String globalId = dataCollectorManager.getGlobalCollectionId();
    if (globalId == null) {
      MessageUtil.sendMessage(sender, Message.DATACOLLECT_GLOBAL_STATUS_FAIL);
      return;
    }
    MessageUtil.sendMessage(sender, Message.DATACOLLECT_GLOBAL_STATUS_HEADER);
    MessageUtil.sendMessage(sender, Message.DATACOLLECT_GLOBAL_STATUS_ACTIVE);
    MessageUtil.sendMessage(
        sender, Message.DATACOLLECT_GLOBAL_STATUS_SESSION_ID, "id", globalId.replace('#', ' '));
    MessageUtil.sendMessage(sender, Message.DATACOLLECT_GLOBAL_STATUS_PLAYERS_HEADER);

    int playerCount = 0;
    for (DataSession session : dataCollectorManager.getActiveSessions().values()) {
      if (globalId.equals(session.getStatus())) {
        long seconds = Duration.between(session.getStartTime(), Instant.now()).toSeconds();
        MessageUtil.sendMessage(
            sender,
            Message.DATACOLLECT_GLOBAL_STATUS_PLAYER_ENTRY,
            "player",
            session.getPlayer(),
            "time",
            String.valueOf(seconds),
            "ticks",
            String.valueOf(session.getRecordedTicks().size()));
        playerCount++;
      }
    }
    if (playerCount == 0) {
      MessageUtil.sendMessage(sender, Message.DATACOLLECT_STATUS_NONE);
    }
  }

  private String resolveStatus(String type, String details, CommandSender sender) {
    return switch (type) {
      case "LEGIT", "CHEAT" -> {
        if (details == null || details.isEmpty()) {
          MessageUtil.sendMessage(sender, Message.DATACOLLECT_DETAILS_REQUIRED);
          yield null;
        }
        yield type + " " + details;
      }
      case "UNLABELED" -> "UNLABELED";
      default -> {
        MessageUtil.sendMessage(sender, Message.DATACOLLECT_INVALID_TYPE);
        yield null;
      }
    };
  }
}
