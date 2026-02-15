/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2026 KaelusMC
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
package space.kaelus.sloth.integration

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldguard.WorldGuard
import java.util.Locale
import java.util.logging.Logger
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.region.RegionProvider

class WorldGuardManager(private val logger: Logger, private val configManager: ConfigManager) :
  RegionProvider {
  private val worldGuardLoaded = Bukkit.getPluginManager().isPluginEnabled("WorldGuard")
  private val worldGuardInstance: WorldGuard? =
    if (worldGuardLoaded) WorldGuard.getInstance() else null

  init {
    if (worldGuardLoaded) {
      logger.info("WorldGuard hook enabled.")
    } else {
      logger.info("WorldGuard not found, hook disabled.")
    }
  }

  override fun isPlayerInDisabledRegion(player: Player): Boolean {
    if (!worldGuardLoaded) {
      return false
    }
    val disabledRegions = configManager.aiDisabledRegions
    if (disabledRegions.isNullOrEmpty()) {
      return false
    }
    val worldGuard = worldGuardInstance ?: return false
    val container = worldGuard.platform.regionContainer
    val regions = container.get(BukkitAdapter.adapt(player.world)) ?: return false

    val set =
      regions.getApplicableRegions(
        BlockVector3.at(player.location.x, player.location.y, player.location.z)
      )

    val playerRegions = set.regions.filterNot { it.id.equals("__global__", ignoreCase = true) }
    if (playerRegions.isEmpty()) {
      return false
    }

    val worldName = player.world.name.lowercase(Locale.ROOT)

    return playerRegions.all { region ->
      val regionId = region.id.lowercase(Locale.ROOT)
      matchesDisabledRegion(regionId, worldName, disabledRegions)
    }
  }

  private fun matchesDisabledRegion(
    regionId: String,
    worldName: String,
    disabledRegions: List<String>,
  ): Boolean {
    for (entry in disabledRegions) {
      if (entry.contains(":")) {
        val parts = entry.split(":", limit = 2)
        val disabledRegionName = parts[0]
        val disabledWorldName = parts[1]
        if (regionId == disabledRegionName && worldName == disabledWorldName) {
          return true
        }
      } else if (regionId == entry) {
        return true
      }
    }
    return false
  }
}
