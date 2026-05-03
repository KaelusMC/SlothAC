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
import io.mockk.verify
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.monitor.MonitorMode
import space.kaelus.sloth.monitor.MonitorNameMode
import space.kaelus.sloth.monitor.MonitorSettings
import space.kaelus.sloth.monitor.MonitorTheme
import space.kaelus.sloth.player.SlothPlayer

class ResilientViolationDatabaseTest {

  @Test
  fun `switches to in-memory fallback after runtime failure`() {
    val configManager = mockk<ConfigManager>(relaxed = true)
    every { configManager.config.getString("history.server-name", any()) } returns "server"
    val logger = mockk<Logger>(relaxed = true)
    val health = DatabaseHealth()
    val database =
      ResilientViolationDatabase(
        primary = AlwaysFailingViolationDatabase(),
        fallback = InMemoryViolationDatabase(configManager),
        health = health,
        logger = logger,
      )
    val playerId = UUID.randomUUID()
    val settings =
      MonitorSettings(
        mode = MonitorMode.COMPACT,
        theme = MonitorTheme.CALM,
        showPing = true,
        showDmg = true,
        showTrend = true,
        showName = MonitorNameMode.AUTO,
      )

    assertEquals(1, database.incrementViolationLevel(playerId, "default"))
    assertFalse(health.isPersistentAvailable())
    assertEquals(1, database.getViolationLevel(playerId, "default"))

    database.saveMonitorSettings(playerId, settings)
    assertEquals(settings, database.loadMonitorSettings(playerId))

    verify(exactly = 1) {
      logger.log(
        Level.WARNING,
        "Persistent database storage failed at runtime. Sloth is switching to in-memory storage.",
        any<Throwable>(),
      )
    }
  }

  @Test
  fun `degrades only once and stops retrying the broken persistent database`() {
    val configManager = mockk<ConfigManager>(relaxed = true)
    every { configManager.config.getString("history.server-name", any()) } returns "server"
    val logger = mockk<Logger>(relaxed = true)
    val health = DatabaseHealth()
    val primary = CountingFailingViolationDatabase()
    val database =
      ResilientViolationDatabase(
        primary = primary,
        fallback = InMemoryViolationDatabase(configManager),
        health = health,
        logger = logger,
      )
    val playerId = UUID.randomUUID()

    assertEquals(1, database.incrementViolationLevel(playerId, "default"))
    assertEquals(1, primary.incrementAttempts.get())
    assertFalse(health.isPersistentAvailable())
    assertNotNull(health.failureCause)

    assertEquals(2, database.incrementViolationLevel(playerId, "default"))
    assertEquals(1, primary.incrementAttempts.get())

    verify(exactly = 1) {
      logger.log(
        Level.WARNING,
        "Persistent database storage failed at runtime. Sloth is switching to in-memory storage.",
        any<Throwable>(),
      )
    }
  }

  private class AlwaysFailingViolationDatabase : ViolationDatabase {
    override fun logAlert(player: SlothPlayer, verbose: String, checkName: String, vls: Int) =
      error("db down")

    override fun getLogCount(player: UUID): Int = error("db down")

    override fun getViolations(player: UUID, page: Int, limit: Int): List<Violation> =
      error("db down")

    override fun getUniqueViolatorsSince(since: Long): Int = error("db down")

    override fun recordLogin(playerUUID: UUID, timestamp: Long) = error("db down")

    override fun countUniquePlayersSince(since: Long): Int = error("db down")

    override fun saveAiBuffer(playerUUID: UUID, buffer: Double, updatedAt: Long) = error("db down")

    override fun loadAiBuffer(playerUUID: UUID): AiBufferState? = error("db down")

    override fun getLogCount(since: Long): Int = error("db down")

    override fun getLogCounts(playerUUIDs: Collection<UUID>): Map<UUID, Int> = error("db down")

    override fun getViolations(page: Int, limit: Int, since: Long): List<Violation> =
      error("db down")

    override fun getViolationLevel(playerUUID: UUID, punishGroupName: String): Int =
      error("db down")

    override fun incrementViolationLevel(playerUUID: UUID, punishGroupName: String): Int =
      error("db down")

    override fun resetViolationLevel(playerUUID: UUID, punishGroupName: String) = error("db down")

    override fun resetAllViolationLevels(playerUUID: UUID) = error("db down")

    override fun loadMonitorSettings(playerUUID: UUID): MonitorSettings? = error("db down")

    override fun saveMonitorSettings(playerUUID: UUID, settings: MonitorSettings) = error("db down")
  }

  private class CountingFailingViolationDatabase : ViolationDatabase {
    val incrementAttempts = AtomicInteger()

    override fun logAlert(player: SlothPlayer, verbose: String, checkName: String, vls: Int) =
      error("db down")

    override fun getLogCount(player: UUID): Int = error("db down")

    override fun getViolations(player: UUID, page: Int, limit: Int): List<Violation> =
      error("db down")

    override fun getUniqueViolatorsSince(since: Long): Int = error("db down")

    override fun recordLogin(playerUUID: UUID, timestamp: Long) = error("db down")

    override fun countUniquePlayersSince(since: Long): Int = error("db down")

    override fun saveAiBuffer(playerUUID: UUID, buffer: Double, updatedAt: Long) = error("db down")

    override fun loadAiBuffer(playerUUID: UUID): AiBufferState? = error("db down")

    override fun getLogCount(since: Long): Int = error("db down")

    override fun getLogCounts(playerUUIDs: Collection<UUID>): Map<UUID, Int> = error("db down")

    override fun getViolations(page: Int, limit: Int, since: Long): List<Violation> =
      error("db down")

    override fun getViolationLevel(playerUUID: UUID, punishGroupName: String): Int =
      error("db down")

    override fun incrementViolationLevel(playerUUID: UUID, punishGroupName: String): Int {
      incrementAttempts.incrementAndGet()
      error("db down")
    }

    override fun resetViolationLevel(playerUUID: UUID, punishGroupName: String) = error("db down")

    override fun resetAllViolationLevels(playerUUID: UUID) = error("db down")

    override fun loadMonitorSettings(playerUUID: UUID): MonitorSettings? = error("db down")

    override fun saveMonitorSettings(playerUUID: UUID, settings: MonitorSettings) = error("db down")
  }
}
