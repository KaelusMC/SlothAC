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
package space.kaelus.sloth.player

import com.github.retrooper.packetevents.protocol.player.User
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.util.UUID
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.plugin.PluginManager
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.alert.AlertManager
import space.kaelus.sloth.api.event.SlothEventBus
import space.kaelus.sloth.checks.CheckManager
import space.kaelus.sloth.checks.impl.ai.DataCollectorManager
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.punishment.PunishmentManager
import space.kaelus.sloth.scheduler.SchedulerService
import space.kaelus.sloth.server.AIServerProvider

class PlayerDataManagerTest {

  @Test
  fun `handleUserLogin skips players with sloth disable permission`() {
    val fixture = createFixture()
    fixture.disablePermission = true

    fixture.manager.handleUserLogin(fixture.user, fixture.player)

    assertNull(fixture.manager.getPlayer(fixture.player))
    assertTrue(fixture.manager.getPlayers().isEmpty())
    verify(exactly = 0) { fixture.checkManagerFactory.create(any()) }
  }

  @Test
  fun `getPlayer keeps tracked player after sloth disable is granted mid-session`() {
    val fixture = createFixture()
    val trackedPlayer = mockk<SlothPlayer>(relaxed = true)
    every { trackedPlayer.player } returns fixture.player

    trackedPlayers(fixture.manager)[fixture.player.uniqueId] = trackedPlayer

    assertNotNull(fixture.manager.getPlayer(fixture.player))

    fixture.disablePermission = true

    assertSame(trackedPlayer, fixture.manager.getPlayer(fixture.player))
    assertSame(trackedPlayer, fixture.manager.getPlayer(fixture.player.uniqueId))
    assertTrue(fixture.manager.getPlayers().contains(trackedPlayer))
    verify(exactly = 0) { fixture.alertManager.handlePlayerQuit(any()) }
  }

  private fun createFixture(): Fixture {
    val plugin = mockk<SlothAC>(relaxed = true)
    val server = mockk<Server>(relaxed = true)
    val pluginManager = mockk<PluginManager>(relaxed = true)
    every { plugin.server } returns server
    every { server.pluginManager } returns pluginManager
    every { pluginManager.registerEvents(any(), plugin) } just runs

    val scheduler = mockk<SchedulerService>(relaxed = true)
    every { scheduler.runSync(any<Player>(), any<Runnable>()) } answers
      {
        secondArg<Runnable>().run()
        mockk(relaxed = true)
      }

    val configManager = mockk<ConfigManager>(relaxed = true)
    every { configManager.aiSequence } returns 40
    every { configManager.cancelDuplicatePacket } returns true
    every { configManager.forceCancelDuplicatePacket } returns false
    every { configManager.ignoreDuplicatePacketRotation } returns true

    val alertManager = mockk<AlertManager>(relaxed = true)
    every { alertManager.handlePlayerQuit(any()) } just runs

    val dataCollectorManager = mockk<DataCollectorManager>(relaxed = true)
    every { dataCollectorManager.getSession(any()) } returns null
    every { dataCollectorManager.stopCollecting(any()) } returns false

    val checkManagerFactory = mockk<CheckManager.Factory>()
    every { checkManagerFactory.create(any()) } returns mockk(relaxed = true)

    val punishmentManagerFactory = mockk<PunishmentManager.Factory>()
    every { punishmentManagerFactory.create(any()) } returns mockk(relaxed = true)

    var disablePermission = false
    val player = mockk<Player>(relaxed = true)
    every { player.uniqueId } returns UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    every { player.isOnline } returns true
    every { player.hasPermission(any<String>()) } answers
      {
        firstArg<String>() == ExemptManager.DISABLE_PERMISSION && disablePermission
      }

    val manager =
      PlayerDataManager(
        plugin = plugin,
        alertManager = alertManager,
        dataCollectorManager = dataCollectorManager,
        configManager = configManager,
        aiServerProvider = mockk<AIServerProvider>(relaxed = true),
        exemptManager = ExemptManager(),
        scheduler = scheduler,
        checkManagerFactory = checkManagerFactory,
        punishmentManagerFactory = punishmentManagerFactory,
        eventBus = mockk<SlothEventBus>(relaxed = true),
      )

    return Fixture(
      manager = manager,
      user = mockk<User>(relaxed = true),
      player = player,
      alertManager = alertManager,
      checkManagerFactory = checkManagerFactory,
      disablePermissionAccessor = { disablePermission },
      disablePermissionMutator = { disablePermission = it },
    )
  }

  private data class Fixture(
    val manager: PlayerDataManager,
    val user: User,
    val player: Player,
    val alertManager: AlertManager,
    val checkManagerFactory: CheckManager.Factory,
    private val disablePermissionAccessor: () -> Boolean,
    private val disablePermissionMutator: (Boolean) -> Unit,
  ) {
    var disablePermission: Boolean
      get() = disablePermissionAccessor()
      set(value) = disablePermissionMutator(value)
  }

  private companion object {
    val playersField =
      PlayerDataManager::class.java.getDeclaredField("players").apply { isAccessible = true }

    @Suppress("UNCHECKED_CAST")
    fun trackedPlayers(manager: PlayerDataManager): MutableMap<UUID, SlothPlayer> {
      return playersField.get(manager) as MutableMap<UUID, SlothPlayer>
    }
  }
}
