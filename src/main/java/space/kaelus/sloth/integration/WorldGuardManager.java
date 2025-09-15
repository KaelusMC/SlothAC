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
package space.kaelus.sloth.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.config.ConfigManager;

public class WorldGuardManager {
  private final SlothAC plugin;
  private final ConfigManager configManager;
  private final boolean worldGuardLoaded;
  private WorldGuard worldGuardInstance;

  public WorldGuardManager(SlothAC plugin, ConfigManager configManager) {
    this.plugin = plugin;
    this.configManager = configManager;
    this.worldGuardLoaded = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
    if (this.worldGuardLoaded) {
      this.worldGuardInstance = WorldGuard.getInstance();
      plugin.getLogger().info("WorldGuard hook enabled.");
    } else {
      plugin.getLogger().info("WorldGuard not found, hook disabled.");
    }
  }

  public boolean isPlayerInDisabledRegion(Player player) {
    if (!worldGuardLoaded) {
      return false;
    }
    List<String> disabledRegions = configManager.getAiDisabledRegions();
    if (disabledRegions == null || disabledRegions.isEmpty()) {
      return false;
    }
    RegionContainer container = worldGuardInstance.getPlatform().getRegionContainer();
    RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
    if (regions == null) {
      return false;
    }

    ApplicableRegionSet set =
        regions.getApplicableRegions(
            BlockVector3.at(
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ()));
    Set<ProtectedRegion> playerRegions = set.getRegions();

    if (playerRegions.isEmpty()) {
      return false;
    }

    return playerRegions.stream()
        .allMatch(region -> disabledRegions.contains(region.getId().toLowerCase(Locale.ROOT)));
  }
}
