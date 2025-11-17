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
package space.kaelus.sloth.punishment;

import java.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.alert.AlertManager;
import space.kaelus.sloth.alert.AlertType;
import space.kaelus.sloth.checks.ICheck;
import space.kaelus.sloth.config.ConfigManager;
import space.kaelus.sloth.database.ViolationDatabase;
import space.kaelus.sloth.player.SlothPlayer;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;

public class PunishmentManager {
  private final SlothPlayer slothPlayer;
  private final SlothAC plugin;
  private final ConfigManager configManager;
  private final Map<String, PunishGroup> punishmentGroups = new HashMap<>();
  private final AlertManager alertManager;
  private final ViolationDatabase database;

  public PunishmentManager(
      SlothPlayer slothPlayer,
      SlothAC plugin,
      ConfigManager configManager,
      ViolationDatabase database,
      AlertManager alertManager) {
    this.slothPlayer = slothPlayer;
    this.plugin = plugin;
    this.configManager = configManager;
    this.alertManager = alertManager;
    this.database = database;
    reload();
  }

  public void reload() {
    punishmentGroups.clear();

    ConfigurationSection punishmentsSection =
        configManager.getPunishments().getConfigurationSection("Punishments");
    if (punishmentsSection == null) return;

    for (String groupName : punishmentsSection.getKeys(false)) {
      ConfigurationSection groupSection = punishmentsSection.getConfigurationSection(groupName);
      if (groupSection == null) continue;

      List<String> checkNamesFilters = groupSection.getStringList("checks");
      ConfigurationSection actionsSection = groupSection.getConfigurationSection("actions");

      if (actionsSection == null) continue;

      NavigableMap<Integer, List<String>> parsedActions = new TreeMap<>();
      for (String vlString : actionsSection.getKeys(false)) {
        try {
          int vl = Integer.parseInt(vlString);
          List<String> commands = actionsSection.getStringList(vlString);
          parsedActions.put(vl, commands);
        } catch (NumberFormatException e) {
          plugin
              .getLogger()
              .warning("Invalid VL " + vlString + " in punishment group " + groupName + ".");
        }
      }

      if (!parsedActions.isEmpty()) {
        PunishGroup punishGroup = new PunishGroup(groupName, checkNamesFilters, parsedActions);
        punishmentGroups.put(groupName, punishGroup);
      }
    }
  }

  public void handleFlag(ICheck check, String debug) {
    if (slothPlayer.getExemptManager().isExempt(slothPlayer.getPlayer())) {
      return;
    }

    for (PunishGroup group : punishmentGroups.values()) {
      if (group.isCheckAssociated(check)) {
        Bukkit.getScheduler()
            .runTaskAsynchronously(
                plugin,
                () -> {
                  int newVl =
                      database.incrementViolationLevel(slothPlayer.getUuid(), group.getGroupName());

                  Map.Entry<Integer, List<String>> entry = group.getActions().floorEntry(newVl);
                  if (entry != null) {
                    executeCommands(check, group, newVl, debug, entry.getValue());
                  }
                });
      }
    }
  }

  private void executeCommands(
      ICheck check, PunishGroup group, int vl, String verbose, List<String> commands) {
    for (String command : commands) {
      String commandLower = command.toLowerCase(Locale.ROOT);

      if (commandLower.equals("[alert]")) {
        sendAlert(check, vl, verbose);
      } else if (commandLower.equals("[log]")) {
        database.logAlert(slothPlayer, verbose, check.getCheckName(), vl);
      } else if (commandLower.equals("[reset]")) {
        database.resetViolationLevel(slothPlayer.getUuid(), group.getGroupName());
      } else if (commandLower.startsWith("[broadcast] ")) {
        final String message = command.substring("[broadcast] ".length());
        final Component component =
            MessageUtil.format(
                message,
                "player",
                slothPlayer.getPlayer().getName(),
                "check_name",
                check.getCheckName(),
                "vl",
                String.valueOf(vl),
                "verbose",
                verbose);

        Bukkit.getScheduler()
            .runTask(
                plugin,
                () -> {
                  plugin.getAdventure().players().sendMessage(component);
                });
      } else {
        String formattedCmd =
            command
                .replace("<player>", slothPlayer.getPlayer().getName())
                .replace("<check_name>", check.getCheckName())
                .replace("<vl>", String.valueOf(vl))
                .replace("<verbose>", verbose);

        Bukkit.getScheduler()
            .runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCmd));
      }
    }
  }

  private void sendAlert(ICheck check, int vl, String verbose) {
    final Component message =
        MessageUtil.getMessage(
            Message.ALERTS_FORMAT,
            "player",
            slothPlayer.getPlayer().getName(),
            "check_name",
            check.getCheckName(),
            "vl",
            String.valueOf(vl),
            "verbose",
            verbose);

    Bukkit.getScheduler()
        .runTask(
            plugin,
            () -> {
              alertManager.send(message, AlertType.REGULAR);
            });
  }
}
