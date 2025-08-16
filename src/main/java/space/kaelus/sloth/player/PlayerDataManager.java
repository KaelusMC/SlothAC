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

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.checks.impl.ai.DataCollectorCheck;
import space.kaelus.sloth.data.DataSession;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager implements Listener {
    private final Map<UUID, SlothPlayer> players = new ConcurrentHashMap<>();

    public PlayerDataManager() {
        SlothAC.getInstance().getServer().getPluginManager().registerEvents(this, SlothAC.getInstance());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        players.put(player.getUniqueId(), new SlothPlayer(player));

        if (player.hasPermission("sloth.alerts") && player.hasPermission("sloth.alerts.enable-on-join")) {
            SlothAC.getInstance().getAlertManager().toggleAlerts(player, true);
        }

        if (player.hasPermission("sloth.brand") && player.hasPermission("sloth.brand.enable-on-join")) {
            SlothAC.getInstance().getAlertManager().toggleBrandAlerts(player, true);
        }

        String globalId = DataCollectorCheck.getGlobalCollectionId();
        if (globalId != null) {
            DataCollectorCheck.startCollecting(player.getUniqueId(), player.getName(), globalId);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        DataSession session = DataCollectorCheck.getSession(uuid);
        if (session != null) {
            String globalId = DataCollectorCheck.getGlobalCollectionId();
            if (globalId == null || !session.getStatus().equals(globalId)) {
                DataCollectorCheck.stopCollecting(uuid);
            }
        }

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
}