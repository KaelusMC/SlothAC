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

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.command.SlothCommand;
import space.kaelus.sloth.config.ConfigManager;
import space.kaelus.sloth.config.LocaleManager;
import space.kaelus.sloth.database.DatabaseManager;
import space.kaelus.sloth.database.Violation;
import space.kaelus.sloth.sender.Sender;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;

public class LogsCommand implements SlothCommand {
  private final SlothAC plugin;
  private final DatabaseManager databaseManager;
  private final ConfigManager configManager;
  private final LocaleManager localeManager;

  public LogsCommand(
      SlothAC plugin,
      DatabaseManager databaseManager,
      ConfigManager configManager,
      LocaleManager localeManager) {
    this.plugin = plugin;
    this.databaseManager = databaseManager;
    this.configManager = configManager;
    this.localeManager = localeManager;
  }

  @Override
  public void register(CommandManager<Sender> manager) {
    manager.command(
        manager
            .commandBuilder("sloth", "slothac")
            .literal("logs")
            .permission("sloth.logs")
            .optional("page", IntegerParser.integerParser(1))
            .flag(manager.flagBuilder("time").withComponent(StringParser.stringParser()))
            .handler(this::handleLogs));
  }

  private void handleLogs(CommandContext<Sender> context) {
    Sender sender = context.sender();
    int page = context.getOrDefault("page", 1);
    String timeArg = context.flags().get("time");

    if (databaseManager.getDatabase() == null
        || !configManager.getConfig().getBoolean("history.enabled", false)) {
      MessageUtil.sendMessage(sender.getNativeSender(), Message.HISTORY_DISABLED);
      return;
    }

    long since = parseTime(timeArg);
    if (since == -1L) {
      MessageUtil.sendMessage(sender.getNativeSender(), Message.LOGS_INVALID_TIME);
      return;
    }

    plugin
        .getServer()
        .getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              int entriesPerPage = 10;
              List<Violation> violations =
                  databaseManager.getDatabase().getViolations(page, entriesPerPage, since);
              int totalLogs = databaseManager.getDatabase().getLogCount(since);
              int maxPages = Math.max(1, (int) Math.ceil((double) totalLogs / entriesPerPage));

              MessageUtil.sendMessage(
                  sender.getNativeSender(),
                  Message.LOGS_HEADER,
                  "page",
                  String.valueOf(page),
                  "max_pages",
                  String.valueOf(maxPages));

              if (violations.isEmpty()) {
                MessageUtil.sendMessage(sender.getNativeSender(), Message.LOGS_NO_VIOLATIONS);
                return;
              }

              for (Violation violation : violations) {
                sender.sendMessage(
                    MessageUtil.getMessage(
                        Message.LOGS_ENTRY,
                        "server",
                        violation.serverName(),
                        "player",
                        violation.playerName(),
                        "check",
                        violation.checkName(),
                        "vl",
                        String.valueOf(violation.vl()),
                        "verbose",
                        violation.verbose(),
                        "timeago",
                        getTimeAgo(violation.createdAt())));
              }
            });
  }

  private long parseTime(String timeArg) {
    if (timeArg == null) {
      return 0L;
    }
    try {
      if (timeArg.length() < 2) return -1L;
      long value = Long.parseLong(timeArg.substring(0, timeArg.length() - 1));
      char unit = Character.toLowerCase(timeArg.charAt(timeArg.length() - 1));
      long multiplier;
      switch (unit) {
        case 'm':
          multiplier = TimeUnit.MINUTES.toMillis(1);
          break;
        case 'h':
          multiplier = TimeUnit.HOURS.toMillis(1);
          break;
        case 'd':
          multiplier = TimeUnit.DAYS.toMillis(1);
          break;
        default:
          return -1L;
      }
      return System.currentTimeMillis() - (value * multiplier);
    } catch (NumberFormatException e) {
      return -1L;
    }
  }

  private String getTimeAgo(Date date) {
    LocaleManager lm = localeManager;
    String ago = lm.getRawMessage(Message.TIME_AGO);
    String d = lm.getRawMessage(Message.TIME_DAYS);
    String h = lm.getRawMessage(Message.TIME_HOURS);
    String m = lm.getRawMessage(Message.TIME_MINUTES);
    String s = lm.getRawMessage(Message.TIME_SECONDS);

    long durationMillis = System.currentTimeMillis() - date.getTime();
    long days = TimeUnit.MILLISECONDS.toDays(durationMillis);
    durationMillis -= TimeUnit.DAYS.toMillis(days);
    long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
    durationMillis -= TimeUnit.HOURS.toMillis(hours);
    long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis);

    if (days > 0) return days + d + ago;
    if (hours > 0) return hours + h + ago;
    if (minutes > 0) return minutes + m + ago;
    return TimeUnit.MILLISECONDS.toSeconds(durationMillis) + s + ago;
  }
}
