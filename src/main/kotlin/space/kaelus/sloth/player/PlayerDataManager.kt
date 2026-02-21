/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2026 KaelusMC
 *
 * This file contains code derived from GrimAC.
 * The original authors of GrimAC are credited below.
 *
 * Copyright (c) 2021-2026 GrimAC, DefineOutside and contributors.
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
package space.kaelus.sloth.player

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.alert.AlertManager
import space.kaelus.sloth.alert.AlertType
import space.kaelus.sloth.api.event.SlothEventBus
import space.kaelus.sloth.checks.CheckManager
import space.kaelus.sloth.checks.impl.ai.DataCollectorManager
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.data.DataSession
import space.kaelus.sloth.punishment.PunishmentManager
import space.kaelus.sloth.scheduler.SchedulerService
import space.kaelus.sloth.server.AIServerProvider

class PlayerDataManager
@Suppress("LongParameterList")
constructor(
  private val plugin: SlothAC,
  private val alertManager: AlertManager,
  private val dataCollectorManager: DataCollectorManager,
  private val configManager: ConfigManager,
  private val aiServerProvider: AIServerProvider,
  private val exemptManager: ExemptManager,
  private val scheduler: SchedulerService,
  private val checkManagerFactory: CheckManager.Factory,
  private val punishmentManagerFactory: PunishmentManager.Factory,
  private val eventBus: SlothEventBus,
) : Listener {
  private val players = ConcurrentHashMap<UUID, SlothPlayer>()

  init {
    plugin.server.pluginManager.registerEvents(this, plugin)
  }

  @EventHandler
  fun onQuit(event: PlayerQuitEvent) {
    val player = event.player
    val uuid = player.uniqueId

    val session: DataSession? = dataCollectorManager.getSession(uuid)
    if (session != null) {
      dataCollectorManager.stopCollecting(uuid)
    }

    alertManager.handlePlayerQuit(player)
    players.remove(uuid)
  }

  fun getPlayer(player: Player?): SlothPlayer? {
    if (player == null) {
      return null
    }
    return players[player.uniqueId]
  }

  fun getPlayer(uuid: UUID): SlothPlayer? {
    return players[uuid]
  }

  fun getPlayers(): Collection<SlothPlayer> {
    return players.values
  }

  fun handleUserLogin(
    user: com.github.retrooper.packetevents.protocol.player.User,
    player: Player,
  ) {
    scheduler.runSync(
      player,
      Runnable {
        if (!player.isOnline || players.containsKey(player.uniqueId)) {
          return@Runnable
        }

        players[player.uniqueId] =
          SlothPlayer(
            player = player,
            user = user,
            plugin = plugin,
            aiSequence = configManager.aiSequence,
            alertManager = alertManager,
            dataCollectorManager = dataCollectorManager,
            aiServerProvider = aiServerProvider,
            exemptManager = exemptManager,
            scheduler = scheduler,
            checkManagerFactory = checkManagerFactory,
            punishmentManagerFactory = punishmentManagerFactory,
            eventBus = eventBus,
          )

        if (
          player.hasPermission("sloth.alerts") &&
            player.hasPermission("sloth.alerts.enable-on-join")
        ) {
          if (!alertManager.hasAlertsEnabled(player, AlertType.REGULAR)) {
            alertManager.toggle(player, AlertType.REGULAR, true)
          }
        }

        if (
          player.hasPermission("sloth.brand") && player.hasPermission("sloth.brand.enable-on-join")
        ) {
          if (!alertManager.hasAlertsEnabled(player, AlertType.BRAND)) {
            alertManager.toggle(player, AlertType.BRAND, true)
          }
        }
      },
    )
  }

  fun handleUserDisconnect(user: com.github.retrooper.packetevents.protocol.player.User) {
    val uuid = user.uuid ?: return
    players.remove(uuid)
  }

  fun reloadAllPlayers() {
    for (slothPlayer in players.values) {
      slothPlayer.reload()
    }
  }
}
