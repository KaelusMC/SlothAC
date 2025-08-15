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

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.IntegerParser;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.command.SlothCommand;
import space.kaelus.sloth.config.LocaleManager;
import space.kaelus.sloth.database.Violation;
import space.kaelus.sloth.sender.Sender;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HistoryCommand implements SlothCommand {

    @Override
    public void register(CommandManager<Sender> manager) {
        manager.command(
                manager.commandBuilder("sloth")
                        .literal("history", "hist")
                        .permission("sloth.history")
                        .required("target", PlayerParser.playerParser())
                        .optional("page", IntegerParser.integerParser(1))
                        .handler(this::handleHistory)
        );
    }

    private void handleHistory(CommandContext<Sender> context) {
        Sender sender = context.sender();
        OfflinePlayer target = context.get("target");
        int page = context.getOrDefault("page", 1);
        SlothAC plugin = SlothAC.getInstance();

        if (plugin.getDatabaseManager().getDatabase() == null || !plugin.getConfigManager().getConfig().getBoolean("history.enabled", false)) {
            MessageUtil.sendMessage(sender.getNativeSender(), Message.HISTORY_DISABLED);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                MessageUtil.sendMessage(sender.getNativeSender(), Message.PLAYER_NOT_FOUND);
                return;
            }

            int entriesPerPage = 10;
            List<Violation> violations = plugin.getDatabaseManager().getDatabase().getViolations(target.getUniqueId(), page, entriesPerPage);
            int totalLogs = plugin.getDatabaseManager().getDatabase().getLogCount(target.getUniqueId());
            int maxPages = Math.max(1, (int) Math.ceil((double) totalLogs / entriesPerPage));

            MessageUtil.sendMessage(sender.getNativeSender(), Message.HISTORY_HEADER,
                    "player", target.getName(),
                    "page", String.valueOf(page),
                    "maxPages", String.valueOf(maxPages)
            );

            if (violations.isEmpty()) {
                MessageUtil.sendMessage(sender.getNativeSender(), Message.HISTORY_NO_VIOLATIONS);
                return;
            }

            for (Violation violation : violations) {
                sender.sendMessage(MessageUtil.getMessage(Message.HISTORY_ENTRY,
                        "server", violation.getServerName(),
                        "check", violation.getCheckName(),
                        "vl", String.valueOf(violation.getVl()),
                        "verbose", violation.getVerbose(),
                        "timeago", getTimeAgo(violation.getCreatedAt()))
                );
            }
        });
    }

    private String getTimeAgo(Date date) {
        LocaleManager lm = SlothAC.getInstance().getLocaleManager();
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