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
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import space.kaelus.sloth.config.ConfigManager;

@Singleton
public class WorldGuardManager {
  private final ConfigManager configManager;
  private final boolean worldGuardLoaded;
  private WorldGuard worldGuardInstance;
  private final Logger logger;

  @Inject
  public WorldGuardManager(Logger logger, ConfigManager configManager) {
    this.configManager = configManager;
    this.logger = logger;
    this.worldGuardLoaded = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
    if (this.worldGuardLoaded) {
      this.worldGuardInstance = WorldGuard.getInstance();
      this.logger.info("WorldGuard hook enabled.");
    } else {
      this.logger.info("WorldGuard not found, hook disabled.");
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

    List<ProtectedRegion> playerRegions =
        set.getRegions().stream().filter(r -> !r.getId().equalsIgnoreCase("__global__")).toList();

    if (playerRegions.isEmpty()) {
      return false;
    }

    final String worldName = player.getWorld().getName().toLowerCase(Locale.ROOT);

    return playerRegions.stream()
        .allMatch(
            region -> {
              final String regionId = region.getId().toLowerCase(Locale.ROOT);

              for (String entry : disabledRegions) {
                if (entry.contains(":")) {
                  String[] parts = entry.split(":", 2);
                  String disabledRegionName = parts[0];
                  String disabledWorldName = parts[1];
                  if (regionId.equals(disabledRegionName) && worldName.equals(disabledWorldName)) {
                    return true;
                  }
                } else {
                  if (regionId.equals(entry)) {
                    return true;
                  }
                }
              }
              return false;
            });
  }
}
