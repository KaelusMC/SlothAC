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
package space.kaelus.sloth.player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.alert.AlertManager;
import space.kaelus.sloth.alert.AlertType;
import space.kaelus.sloth.checks.CheckManager;
import space.kaelus.sloth.checks.impl.ai.DataCollectorManager;
import space.kaelus.sloth.config.ConfigManager;
import space.kaelus.sloth.data.DataSession;
import space.kaelus.sloth.integration.WorldGuardManager;
import space.kaelus.sloth.punishment.PunishmentManager;
import space.kaelus.sloth.server.AIServerProvider;

@Singleton
public class PlayerDataManager implements Listener {
  private final SlothAC plugin;
  private final AlertManager alertManager;
  private final DataCollectorManager dataCollectorManager;
  private final ConfigManager configManager;
  private final WorldGuardManager worldGuardManager;
  private final AIServerProvider aiServerProvider;
  private final ExemptManager exemptManager;
  private final CheckManager.Factory checkManagerFactory;
  private final PunishmentManager.Factory punishmentManagerFactory;

  private final Map<UUID, SlothPlayer> players = new ConcurrentHashMap<>();

  @Inject
  public PlayerDataManager(
      SlothAC plugin,
      AlertManager alertManager,
      DataCollectorManager dataCollectorManager,
      ConfigManager configManager,
      AIServerProvider aiServerProvider,
      WorldGuardManager worldGuardManager,
      ExemptManager exemptManager,
      CheckManager.Factory checkManagerFactory,
      PunishmentManager.Factory punishmentManagerFactory) {
    this.plugin = plugin;
    this.alertManager = alertManager;
    this.dataCollectorManager = dataCollectorManager;
    this.configManager = configManager;
    this.aiServerProvider = aiServerProvider;
    this.worldGuardManager = worldGuardManager;
    this.exemptManager = exemptManager;
    this.checkManagerFactory = checkManagerFactory;
    this.punishmentManagerFactory = punishmentManagerFactory;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    players.put(
        player.getUniqueId(),
        new SlothPlayer(
            player,
            plugin,
            configManager,
            alertManager,
            dataCollectorManager,
            aiServerProvider,
            worldGuardManager,
            exemptManager,
            checkManagerFactory,
            punishmentManagerFactory));

    if (player.hasPermission("sloth.alerts")
        && player.hasPermission("sloth.alerts.enable-on-join")) {
      if (!alertManager.hasAlertsEnabled(player, AlertType.REGULAR)) {
        alertManager.toggle(player, AlertType.REGULAR, true);
      }
    }

    if (player.hasPermission("sloth.brand") && player.hasPermission("sloth.brand.enable-on-join")) {
      if (!alertManager.hasAlertsEnabled(player, AlertType.BRAND)) {
        alertManager.toggle(player, AlertType.BRAND, true);
      }
    }

    String globalId = dataCollectorManager.getGlobalCollectionId();
    if (globalId != null) {
      dataCollectorManager.startCollecting(player.getUniqueId(), player.getName(), globalId);
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    UUID uuid = event.getPlayer().getUniqueId();

    DataSession session = dataCollectorManager.getSession(uuid);
    if (session != null) {
      String globalId = dataCollectorManager.getGlobalCollectionId();
      if (globalId == null || !session.getStatus().equals(globalId)) {
        dataCollectorManager.stopCollecting(uuid);
      }
    }

    alertManager.handlePlayerQuit(player);
    players.remove(uuid);
  }

  public SlothPlayer getPlayer(Player player) {
    if (player == null) {
      return null;
    }
    return players.get(player.getUniqueId());
  }

  public SlothPlayer getPlayer(UUID uuid) {
    return players.get(uuid);
  }

  public Collection<SlothPlayer> getPlayers() {
    return players.values();
  }

  public void reloadAllPlayers() {
    for (SlothPlayer slothPlayer : players.values()) {
      slothPlayer.reload();
    }
  }
}
