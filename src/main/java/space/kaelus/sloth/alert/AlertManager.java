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
package space.kaelus.sloth.alert;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.config.ConfigManager;
import space.kaelus.sloth.config.LocaleManager;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;

public class AlertManager {

  private final SlothAC plugin;
  private final ConfigManager configManager;
  private final LocaleManager localeManager;
  private final BukkitAudiences adventure;

  private final Set<UUID> playersWithAlerts = new CopyOnWriteArraySet<>();
  private final Set<UUID> playersWithBrandAlerts = new CopyOnWriteArraySet<>();
  private final Set<UUID> playersWithSuspiciousAlerts = new CopyOnWriteArraySet<>();
  @Getter private boolean consoleAlertsEnabled = true;
  @Getter private boolean consoleBrandAlertsEnabled = true;
  private boolean logToConsole;

  @Getter private String alertFormat;
  @Getter private String brandAlertFormat;

  public AlertManager(
      SlothAC plugin,
      ConfigManager configManager,
      LocaleManager localeManager,
      BukkitAudiences adventure) {
    this.plugin = plugin;
    this.configManager = configManager;
    this.localeManager = localeManager;
    this.adventure = adventure;
    reload();
  }

  public void reload() {
    this.logToConsole = configManager.getConfig().getBoolean("alerts.print-to-console", true);
    this.alertFormat = localeManager.getRawMessage(Message.ALERTS_FORMAT);
    this.brandAlertFormat = localeManager.getRawMessage(Message.BRAND_NOTIFICATION);
  }

  public boolean hasAlertsEnabled(Player player) {
    return playersWithAlerts.contains(player.getUniqueId());
  }

  public void toggleAlerts(Player player, boolean silent) {
    toggle(player, playersWithAlerts, Message.ALERTS_ENABLED, Message.ALERTS_DISABLED, silent);
  }

  public void toggleConsoleAlerts() {
    this.consoleAlertsEnabled = !this.consoleAlertsEnabled;
  }

  public boolean hasBrandAlertsEnabled(Player player) {
    return playersWithBrandAlerts.contains(player.getUniqueId());
  }

  public void toggleBrandAlerts(Player player, boolean silent) {
    toggle(
        player,
        playersWithBrandAlerts,
        Message.BRAND_ALERTS_ENABLED,
        Message.BRAND_ALERTS_DISABLED,
        silent);
  }

  public void toggleConsoleBrandAlerts() {
    this.consoleBrandAlertsEnabled = !this.consoleBrandAlertsEnabled;
  }

  public void toggleSuspiciousAlerts(Player player) {
    UUID uuid = player.getUniqueId();
    if (playersWithSuspiciousAlerts.contains(uuid)) {
      playersWithSuspiciousAlerts.remove(uuid);
      MessageUtil.sendMessage(player, Message.SUSPICIOUS_ALERTS_DISABLED);
    } else {
      playersWithSuspiciousAlerts.add(uuid);
      MessageUtil.sendMessage(player, Message.SUSPICIOUS_ALERTS_ENABLED);
    }
  }

  public void sendSuspiciousAlert(Component component) {
    for (UUID uuid : playersWithSuspiciousAlerts) {
      Player p = Bukkit.getPlayer(uuid);
      if (p != null && p.hasPermission("sloth.suspicious.alerts")) {
        adventure(p).sendMessage(component);
      }
    }
  }

  public void handlePlayerQuit(Player player) {
    UUID uuid = player.getUniqueId();
    playersWithAlerts.remove(uuid);
    playersWithBrandAlerts.remove(uuid);
    playersWithSuspiciousAlerts.remove(uuid);
  }

  public void sendAlert(Component component) {
    send(component, playersWithAlerts, "sloth.alerts", consoleAlertsEnabled);
  }

  public void sendBrandAlert(Component component) {
    send(component, playersWithBrandAlerts, "sloth.brand", consoleBrandAlertsEnabled);
  }

  private void toggle(
      Player player,
      Set<UUID> playersSet,
      Message enabledMsg,
      Message disabledMsg,
      boolean silent) {
    UUID uuid = player.getUniqueId();
    if (playersSet.contains(uuid)) {
      playersSet.remove(uuid);
      if (!silent) {
        adventure(player).sendMessage(MessageUtil.getMessage(disabledMsg));
      }
    } else {
      playersSet.add(uuid);
      if (!silent) {
        adventure(player).sendMessage(MessageUtil.getMessage(enabledMsg));
      }
    }
  }

  private void send(
      Component component, Set<UUID> playersSet, String permission, boolean consoleFlag) {
    for (UUID uuid : playersSet) {
      Player p = Bukkit.getPlayer(uuid);
      if (p != null && p.hasPermission(permission)) {
        adventure(p).sendMessage(component);
      }
    }

    if (logToConsole && consoleFlag) {
      adventure(Bukkit.getConsoleSender()).sendMessage(component);
    }
  }

  private Audience adventure(Player player) {
    return adventure.player(player);
  }

  private Audience adventure(CommandSender sender) {
    return adventure.sender(sender);
  }
}
