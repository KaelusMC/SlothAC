/*
 * This file is part of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2026 GrimAC, DefineOutside and contributors
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
package space.kaelus.sloth.platform.scheduler.bukkit

import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import space.kaelus.sloth.platform.scheduler.EntityScheduler
import space.kaelus.sloth.platform.scheduler.TaskHandle

class BukkitEntityScheduler : EntityScheduler {
  private val scheduler = Bukkit.getScheduler()

  override fun execute(
    entity: Entity,
    plugin: Plugin,
    run: Runnable,
    retired: Runnable?,
    delay: Long,
  ) {
    scheduler.runTaskLater(plugin, run, delay)
  }

  override fun run(entity: Entity, plugin: Plugin, task: Runnable, retired: Runnable?): TaskHandle {
    return BukkitTaskHandle(scheduler.runTask(plugin, task))
  }

  override fun runDelayed(
    entity: Entity,
    plugin: Plugin,
    task: Runnable,
    retired: Runnable?,
    delayTicks: Long,
  ): TaskHandle {
    return BukkitTaskHandle(scheduler.runTaskLater(plugin, task, delayTicks))
  }

  override fun runAtFixedRate(
    entity: Entity,
    plugin: Plugin,
    task: Runnable,
    retired: Runnable?,
    initialDelayTicks: Long,
    periodTicks: Long,
  ): TaskHandle {
    return BukkitTaskHandle(scheduler.runTaskTimer(plugin, task, initialDelayTicks, periodTicks))
  }
}
