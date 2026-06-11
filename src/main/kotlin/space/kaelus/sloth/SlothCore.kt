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
package space.kaelus.sloth

import com.github.retrooper.packetevents.PacketEvents
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.plugin.ServicePriority
import space.kaelus.sloth.alert.AlertManager
import space.kaelus.sloth.api.SlothApi
import space.kaelus.sloth.command.CommandManager
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.config.LocaleManager
import space.kaelus.sloth.coroutines.SlothCoroutines
import space.kaelus.sloth.database.DatabaseManager
import space.kaelus.sloth.debug.DebugManager
import space.kaelus.sloth.event.DamageEvent
import space.kaelus.sloth.monitor.MonitorViewService
import space.kaelus.sloth.packet.PacketListener
import space.kaelus.sloth.player.PlayerDataManager
import space.kaelus.sloth.redis.CrossServerAlertService
import space.kaelus.sloth.redis.CrossServerSuspiciousService
import space.kaelus.sloth.redis.RedisManager
import space.kaelus.sloth.scheduler.SchedulerService
import space.kaelus.sloth.server.AIServerProvider
import space.kaelus.sloth.utils.MessageUtil

class SlothCore
@Suppress("LongParameterList")
constructor(
  private val plugin: SlothAC,
  private val playerDataManager: PlayerDataManager,
  private val configManager: ConfigManager,
  private val localeManager: LocaleManager,
  private val aiServerProvider: AIServerProvider,
  private val commandManager: CommandManager,
  private val alertManager: AlertManager,
  private val databaseManager: DatabaseManager,
  private val redisManager: RedisManager,
  private val crossServerAlertService: CrossServerAlertService,
  private val crossServerSuspiciousService: CrossServerSuspiciousService,
  private val debugManager: DebugManager,
  private val packetListener: PacketListener,
  private val monitorViewService: MonitorViewService,
  private val damageEvent: DamageEvent,
  private val slothApi: SlothApi,
  private val adventure: BukkitAudiences,
  private val coroutines: SlothCoroutines,
  private val scheduler: SchedulerService,
) {
  fun enable() {
    commandManager.registerCommands()

    MessageUtil.init(localeManager, adventure, plugin.logger)

    initializePacketRuntime()
    plugin.server.pluginManager.registerEvents(damageEvent, plugin)
    plugin.server.servicesManager.register(
      SlothApi::class.java,
      slothApi,
      plugin,
      ServicePriority.Normal,
    )
    scheduler.runAsync {
      crossServerAlertService.start()
      crossServerSuspiciousService.start()
    }
  }

  fun disable() {
    plugin.server.servicesManager.unregister(SlothApi::class.java, slothApi)
    runCatching { playerDataManager.saveAllBuffersSync() }
    runCatching { aiServerProvider.shutdownTransport() }
    runCatching { crossServerAlertService.shutdown() }
    runCatching { crossServerSuspiciousService.shutdown() }
    runCatching { redisManager.shutdown() }
    adventure.close()
    coroutines.close()
    databaseManager.shutdown()
  }

  fun reload() {
    configManager.reloadConfig()
    localeManager.reload()
    debugManager.reload()
    alertManager.reload()
    aiServerProvider.reload()
    playerDataManager.reloadAllPlayers()
    monitorViewService.reload()
    crossServerAlertService.shutdown()
    crossServerSuspiciousService.shutdown()
    scheduler.runAsync {
      redisManager.shutdown()
      crossServerAlertService.start()
      crossServerSuspiciousService.start()
    }
  }

  private fun initializePacketRuntime() {
    PacketEvents.getAPI().eventManager.registerListener(packetListener)
    monitorViewService.start()
    PacketEvents.getAPI().init()
  }
}
