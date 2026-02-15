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

import java.util.concurrent.TimeUnit
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import space.kaelus.sloth.platform.scheduler.AsyncScheduler
import space.kaelus.sloth.platform.scheduler.PlatformScheduler
import space.kaelus.sloth.platform.scheduler.TaskHandle

class BukkitAsyncScheduler : AsyncScheduler {
  private val bukkitScheduler = Bukkit.getScheduler()

  override fun runNow(plugin: Plugin, task: Runnable): TaskHandle {
    return BukkitTaskHandle(bukkitScheduler.runTaskAsynchronously(plugin, task))
  }

  override fun runDelayed(
    plugin: Plugin,
    task: Runnable,
    delay: Long,
    timeUnit: TimeUnit,
  ): TaskHandle {
    return BukkitTaskHandle(
      bukkitScheduler.runTaskLaterAsynchronously(
        plugin,
        task,
        PlatformScheduler.convertTimeToTicks(delay, timeUnit),
      )
    )
  }

  override fun runAtFixedRate(
    plugin: Plugin,
    task: Runnable,
    delay: Long,
    period: Long,
    timeUnit: TimeUnit,
  ): TaskHandle {
    return BukkitTaskHandle(
      bukkitScheduler.runTaskTimerAsynchronously(
        plugin,
        task,
        PlatformScheduler.convertTimeToTicks(delay, timeUnit),
        PlatformScheduler.convertTimeToTicks(period, timeUnit),
      )
    )
  }

  override fun runAtFixedRate(
    plugin: Plugin,
    task: Runnable,
    initialDelayTicks: Long,
    periodTicks: Long,
  ): TaskHandle {
    return BukkitTaskHandle(
      bukkitScheduler.runTaskTimerAsynchronously(plugin, task, initialDelayTicks, periodTicks)
    )
  }

  override fun cancel(plugin: Plugin) {
    bukkitScheduler.cancelTasks(plugin)
  }
}
