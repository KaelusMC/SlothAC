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
package space.kaelus.sloth.monitor

import com.github.retrooper.packetevents.PacketEvents
import io.papermc.paper.event.player.PlayerTrackEntityEvent
import io.papermc.paper.event.player.PlayerUntrackEntityEvent
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerHideEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerShowEntityEvent
import org.bukkit.event.server.PluginDisableEvent
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.player.PlayerDataManager
import space.kaelus.sloth.scheduler.SchedulerService

class MonitorViewService(
  private val plugin: SlothAC,
  playerDataManager: PlayerDataManager,
  private val configManager: ConfigManager,
  scheduler: SchedulerService,
) : Listener {
  private val coordinator = ViewSessionCoordinator(plugin, playerDataManager, scheduler)
  private val conflictObserver =
    ViewConflictObserver(scheduler, coordinator) { viewerId ->
      resolveActiveViewViewer(viewerId, coordinator)
    }

  @Volatile private var runtimeConfig = ViewRuntimeConfig.from(configManager.monitorConfig)
  private var packetHooksRegistered = false

  init {
    plugin.server.pluginManager.registerEvents(this, plugin)
  }

  fun start() {
    if (packetHooksRegistered) {
      return
    }
    PacketEvents.getAPI().eventManager.registerListener(conflictObserver)
    packetHooksRegistered = true
  }

  fun toggle(viewer: Player): Boolean {
    val viewerId = viewer.uniqueId
    return if (coordinator.session(viewerId) != null) {
      coordinator.disable(viewerId, viewer)
      false
    } else {
      enable(viewer)
      true
    }
  }

  fun enable(viewer: Player) {
    coordinator.enable(viewer, runtimeConfig)
  }

  fun reload() {
    runtimeConfig = ViewRuntimeConfig.from(configManager.monitorConfig)
    coordinator.reload(runtimeConfig)
  }

  @EventHandler
  fun onPlayerTrackEntity(event: PlayerTrackEntityEvent) {
    val target = event.entity as? Player ?: return
    val viewer = event.player
    if (isTrackableViewTarget(viewer, target)) {
      coordinator.trackTarget(viewer.uniqueId, target)
    }
  }

  @EventHandler
  fun onPlayerUntrackEntity(event: PlayerUntrackEntityEvent) {
    val target = event.entity as? Player ?: return
    coordinator.removeTrackedTarget(event.player.uniqueId, target.uniqueId, target.name)
  }

  @EventHandler
  fun onPlayerHideEntity(event: PlayerHideEntityEvent) {
    val target = event.entity as? Player ?: return
    coordinator.removeTrackedTarget(event.player.uniqueId, target.uniqueId, target.name)
  }

  @EventHandler
  fun onPlayerShowEntity(event: PlayerShowEntityEvent) {
    val target = event.entity as? Player ?: return
    val viewer = event.player
    if (isTrackableViewTarget(viewer, target)) {
      coordinator.trackTarget(viewer.uniqueId, target)
    }
  }

  @EventHandler
  fun onPlayerQuit(event: PlayerQuitEvent) {
    val left = event.player
    coordinator.disable(left.uniqueId, left)
    coordinator.removeTargetFromAllSessions(left.uniqueId, left.name)
  }

  @EventHandler
  fun onPluginDisable(event: PluginDisableEvent) {
    if (event.plugin !== plugin) {
      return
    }
    for (viewerId in coordinator.activeViewerIds) {
      coordinator.disable(viewerId, Bukkit.getPlayer(viewerId))
    }
  }
}

internal const val VIEW_PERMISSION = "sloth.view"

private fun isTrackableViewTarget(viewer: Player, target: Player): Boolean {
  return target.isOnline && viewer.world.uid == target.world.uid && viewer.canSee(target)
}

private fun resolveActiveViewViewer(viewerId: UUID, coordinator: ViewSessionCoordinator): Player? {
  val viewer = Bukkit.getPlayer(viewerId)
  return when {
    viewer == null || !viewer.isOnline -> {
      coordinator.disable(viewerId, null)
      null
    }
    !viewer.hasPermission(VIEW_PERMISSION) -> {
      coordinator.disable(viewerId, viewer)
      null
    }
    else -> viewer
  }
}
