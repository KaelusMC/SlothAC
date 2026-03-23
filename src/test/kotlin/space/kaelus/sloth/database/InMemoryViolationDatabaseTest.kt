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
package space.kaelus.sloth.database

import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.bukkit.entity.Player
import org.junit.jupiter.api.Test
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.monitor.MonitorMode
import space.kaelus.sloth.monitor.MonitorNameMode
import space.kaelus.sloth.monitor.MonitorSettings
import space.kaelus.sloth.monitor.MonitorTheme
import space.kaelus.sloth.player.SlothPlayer

class InMemoryViolationDatabaseTest {

  @Test
  fun `stores alerts in memory with working counts paging and time filters`() {
    val configManager = mockk<ConfigManager>(relaxed = true)
    every { configManager.config.getString("history.server-name", any()) } returns "test-server"
    val database = InMemoryViolationDatabase(configManager)
    val firstPlayer = createPlayer(UUID.fromString("00000000-0000-0000-0000-000000000001"), "Alpha")
    val secondPlayer =
      createPlayer(UUID.fromString("00000000-0000-0000-0000-000000000002"), "Bravo")

    database.logAlert(firstPlayer, "first", "Aim", 1)
    val afterFirstInsert =
      database
        .getViolations(firstPlayer.uuid, page = 1, limit = 1)
        .single()
        .createdAt
        .toEpochMilli() + 1
    Thread.sleep(5)
    database.logAlert(secondPlayer, "second", "Reach", 2)

    assertEquals(1, database.getLogCount(firstPlayer.uuid))
    assertEquals(1, database.getLogCount(secondPlayer.uuid))
    assertEquals(2, database.getLogCount(0))
    assertEquals(1, database.getLogCount(afterFirstInsert))
    assertEquals(2, database.getUniqueViolatorsSince(0))
    assertEquals(1, database.getUniqueViolatorsSince(afterFirstInsert))
    assertEquals(
      1,
      database.getLogCounts(listOf(firstPlayer.uuid, secondPlayer.uuid))[firstPlayer.uuid],
    )
    assertEquals(
      listOf("second", "first"),
      database.getViolations(page = 1, limit = 10, since = 0).map(Violation::verbose),
    )
    assertEquals(
      listOf("first"),
      database.getViolations(firstPlayer.uuid, page = 1, limit = 10).map(Violation::verbose),
    )
  }

  @Test
  fun `stores punishment levels and monitor settings in degraded runtime`() {
    val configManager = mockk<ConfigManager>(relaxed = true)
    every { configManager.config.getString("history.server-name", any()) } returns "test-server"
    val database = InMemoryViolationDatabase(configManager)
    val playerId = UUID.randomUUID()
    val settings =
      MonitorSettings(
        mode = MonitorMode.COMPACT,
        theme = MonitorTheme.CALM,
        showPing = true,
        showDmg = true,
        showTrend = false,
        showName = MonitorNameMode.AUTO,
      )

    assertEquals(1, database.incrementViolationLevel(playerId, "default"))
    assertEquals(2, database.incrementViolationLevel(playerId, "default"))
    assertEquals(2, database.getViolationLevel(playerId, "default"))

    database.saveMonitorSettings(playerId, settings)
    assertEquals(settings, database.loadMonitorSettings(playerId))

    database.resetViolationLevel(playerId, "default")
    assertEquals(0, database.getViolationLevel(playerId, "default"))

    database.incrementViolationLevel(playerId, "combat")
    database.incrementViolationLevel(playerId, "movement")
    database.resetAllViolationLevels(playerId)
    assertEquals(0, database.getViolationLevel(playerId, "combat"))
    assertEquals(0, database.getViolationLevel(playerId, "movement"))
    assertNull(database.loadMonitorSettings(UUID.randomUUID()))
  }

  private fun createPlayer(uuid: UUID, name: String): SlothPlayer {
    val bukkitPlayer = mockk<Player>(relaxed = true)
    every { bukkitPlayer.name } returns name

    val slothPlayer = mockk<SlothPlayer>(relaxed = true)
    every { slothPlayer.uuid } returns uuid
    every { slothPlayer.player } returns bukkitPlayer
    return slothPlayer
  }
}
