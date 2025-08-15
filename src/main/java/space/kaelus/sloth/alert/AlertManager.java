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

import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

public class AlertManager {

    private final Set<UUID> playersWithAlerts = new CopyOnWriteArraySet<>();
    private final Set<UUID> playersWithBrandAlerts = new CopyOnWriteArraySet<>();
    @Getter
    private boolean consoleAlertsEnabled = true;
    @Getter
    private boolean consoleBrandAlertsEnabled = true;
    private boolean logToConsole;

    @Getter
    private String alertFormat;
    @Getter
    private String brandAlertFormat;

    public AlertManager() {
        reload();
    }

    public void reload() {
        this.logToConsole = SlothAC.getInstance().getConfigManager().getConfig().getBoolean("alerts.print-to-console", true);
        this.alertFormat = SlothAC.getInstance().getLocaleManager().getRawMessage(Message.ALERTS_FORMAT);
        this.brandAlertFormat = SlothAC.getInstance().getLocaleManager().getRawMessage(Message.BRAND_NOTIFICATION);
    }

    public boolean hasAlertsEnabled(Player player) {
        return playersWithAlerts.contains(player.getUniqueId());
    }

    public void toggleAlerts(Player player, boolean silent) {
        UUID uuid = player.getUniqueId();
        if (playersWithAlerts.contains(uuid)) {
            playersWithAlerts.remove(uuid);
            if (!silent) {
                adventure(player).sendMessage(MessageUtil.getMessage(Message.ALERTS_DISABLED));
            }
        } else {
            playersWithAlerts.add(uuid);
            if (!silent) {
                adventure(player).sendMessage(MessageUtil.getMessage(Message.ALERTS_ENABLED));
            }
        }
    }

    public void toggleConsoleAlerts() {
        this.consoleAlertsEnabled = !this.consoleAlertsEnabled;
    }

    public boolean hasBrandAlertsEnabled(Player player) {
        return playersWithBrandAlerts.contains(player.getUniqueId());
    }

    public void toggleBrandAlerts(Player player, boolean silent) {
        UUID uuid = player.getUniqueId();
        if (playersWithBrandAlerts.contains(uuid)) {
            playersWithBrandAlerts.remove(uuid);
            if (!silent) {
                adventure(player).sendMessage(MessageUtil.getMessage(Message.BRAND_ALERTS_DISABLED));
            }
        } else {
            playersWithBrandAlerts.add(uuid);
            if (!silent) {
                adventure(player).sendMessage(MessageUtil.getMessage(Message.BRAND_ALERTS_ENABLED));
            }
        }
    }

    public void toggleConsoleBrandAlerts() {
        this.consoleBrandAlertsEnabled = !this.consoleBrandAlertsEnabled;
    }

    public void handlePlayerQuit(Player player) {
        playersWithAlerts.remove(player.getUniqueId());
        playersWithBrandAlerts.remove(player.getUniqueId());
    }

    public void sendAlert(Component component) {
        for (UUID uuid : playersWithAlerts) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.hasPermission("sloth.alerts")) {
                adventure(p).sendMessage(component);
            }
        }

        if (logToConsole && consoleAlertsEnabled) {
            adventure(Bukkit.getConsoleSender()).sendMessage(component);
        }
    }

    public void sendBrandAlert(Component component) {
        for (UUID uuid : playersWithBrandAlerts) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.hasPermission("sloth.brand")) {
                adventure(p).sendMessage(component);
            }
        }

        if (logToConsole && consoleBrandAlertsEnabled) {
            adventure(Bukkit.getConsoleSender()).sendMessage(component);
        }
    }

    private Audience adventure(Player player) {
        return SlothAC.getInstance().getAdventure().player(player);
    }

    private Audience adventure(CommandSender sender) {
        return SlothAC.getInstance().getAdventure().sender(sender);
    }
}