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

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.OfflinePlayerParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.IntegerParser;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.command.SlothCommand;
import space.kaelus.sloth.config.ConfigManager;
import space.kaelus.sloth.config.LocaleManager;
import space.kaelus.sloth.database.DatabaseManager;
import space.kaelus.sloth.database.Violation;
import space.kaelus.sloth.sender.Sender;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;
import space.kaelus.sloth.utils.TimeUtil;

public class HistoryCommand implements SlothCommand {

  private final SlothAC plugin;
  private final DatabaseManager databaseManager;
  private final ConfigManager configManager;
  private final LocaleManager localeManager;

  public HistoryCommand(
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
            .literal("history", "hist")
            .permission("sloth.history")
            .required("target", OfflinePlayerParser.offlinePlayerParser())
            .optional("page", IntegerParser.integerParser(1))
            .handler(this::handleHistory));
  }

  private void handleHistory(CommandContext<Sender> context) {
    Sender sender = context.sender();
    OfflinePlayer target = context.get("target");
    int page = context.getOrDefault("page", 1);

    if (databaseManager.getDatabase() == null
        || !configManager.getConfig().getBoolean("history.enabled", false)) {
      MessageUtil.sendMessage(sender.getNativeSender(), Message.HISTORY_DISABLED);
      return;
    }

    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              if (!target.hasPlayedBefore() && !target.isOnline()) {
                MessageUtil.sendMessage(sender.getNativeSender(), Message.PLAYER_NOT_FOUND);
                return;
              }

              int entriesPerPage = 10;
              List<Violation> violations =
                  databaseManager
                      .getDatabase()
                      .getViolations(target.getUniqueId(), page, entriesPerPage);
              int totalLogs = databaseManager.getDatabase().getLogCount(target.getUniqueId());
              int maxPages = Math.max(1, (int) Math.ceil((double) totalLogs / entriesPerPage));

              MessageUtil.sendMessage(
                  sender.getNativeSender(),
                  Message.HISTORY_HEADER,
                  "player",
                  target.getName(),
                  "page",
                  String.valueOf(page),
                  "max_pages",
                  String.valueOf(maxPages));

              if (violations.isEmpty()) {
                MessageUtil.sendMessage(sender.getNativeSender(), Message.HISTORY_NO_VIOLATIONS);
                return;
              }

              for (Violation violation : violations) {
                sender.sendMessage(
                    MessageUtil.getMessage(
                        Message.HISTORY_ENTRY,
                        "server",
                        violation.serverName(),
                        "check",
                        violation.checkName(),
                        "vl",
                        String.valueOf(violation.vl()),
                        "verbose",
                        violation.verbose(),
                        "timeago",
                        TimeUtil.formatTimeAgo(violation.createdAt(), localeManager)));
              }
            });
  }
}
