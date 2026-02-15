/*
 * This file is part of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2026 GrimAC, DefineOutside and contributors
 *
 * This file contains code derived from packetevents.
 * Copyright (C) 2024 retrooper and contributors.
 *
 * GrimAC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GrimAC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package space.kaelus.sloth.platform.scheduler

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.plugin.Plugin

/** Represents a scheduler for executing region tasks. */
interface RegionScheduler {
  fun execute(plugin: Plugin, world: World, chunkX: Int, chunkZ: Int, task: Runnable)

  fun execute(plugin: Plugin, location: Location, task: Runnable)

  fun run(plugin: Plugin, world: World, chunkX: Int, chunkZ: Int, task: Runnable): TaskHandle

  fun run(plugin: Plugin, location: Location, task: Runnable): TaskHandle

  fun runDelayed(
    plugin: Plugin,
    world: World,
    chunkX: Int,
    chunkZ: Int,
    task: Runnable,
    delayTicks: Long,
  ): TaskHandle

  fun runDelayed(plugin: Plugin, location: Location, task: Runnable, delayTicks: Long): TaskHandle

  fun runAtFixedRate(
    plugin: Plugin,
    world: World,
    chunkX: Int,
    chunkZ: Int,
    task: Runnable,
    initialDelayTicks: Long,
    periodTicks: Long,
  ): TaskHandle

  fun runAtFixedRate(
    plugin: Plugin,
    location: Location,
    task: Runnable,
    initialDelayTicks: Long,
    periodTicks: Long,
  ): TaskHandle
}
