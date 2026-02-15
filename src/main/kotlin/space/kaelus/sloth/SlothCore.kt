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
import space.kaelus.sloth.packet.PacketListener
import space.kaelus.sloth.player.PlayerDataManager
import space.kaelus.sloth.server.AIServerProvider
import space.kaelus.sloth.utils.MessageUtil

class SlothCore(
  private val plugin: SlothAC,
  private val playerDataManager: PlayerDataManager,
  private val configManager: ConfigManager,
  private val localeManager: LocaleManager,
  private val aiServerProvider: AIServerProvider,
  private val commandManager: CommandManager,
  private val alertManager: AlertManager,
  private val databaseManager: DatabaseManager,
  private val debugManager: DebugManager,
  private val packetListener: PacketListener,
  private val damageEvent: DamageEvent,
  private val slothApi: SlothApi,
  private val adventure: BukkitAudiences,
  private val coroutines: SlothCoroutines,
) {
  fun enable() {
    commandManager.registerCommands()

    MessageUtil.init(localeManager, adventure, plugin.logger)

    try {
      PacketEvents.getAPI().eventManager.registerListener(packetListener)
      PacketEvents.getAPI().init()
    } catch (e: Exception) {
      plugin.logger.severe("Failed to initialize PacketEvents.")
      e.printStackTrace()
    }

    plugin.server.pluginManager.registerEvents(damageEvent, plugin)
    plugin.server.servicesManager.register(
      SlothApi::class.java,
      slothApi,
      plugin,
      ServicePriority.Normal,
    )
  }

  fun disable() {
    plugin.server.servicesManager.unregister(SlothApi::class.java, slothApi)
    adventure.close()
    coroutines.close()
    databaseManager.shutdown()
    if (PacketEvents.getAPI().isInitialized) {
      PacketEvents.getAPI().terminate()
    }
  }

  fun reload() {
    configManager.reloadConfig()
    localeManager.reload()
    debugManager.reload()
    alertManager.reload()
    aiServerProvider.reload()
    playerDataManager.reloadAllPlayers()
  }
}
