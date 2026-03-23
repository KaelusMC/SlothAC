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

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.mockk.mockk
import io.mockk.verify
import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test

private const val TRANSITIONAL_V1_CHECKSUM = 1087069697

class DatabaseMigrationExecutorTest {

  @Test
  fun `baselines legacy sqlite schema and migrates it to the current shape`() {
    val jdbcUrl = createJdbcUrl("slothac-executor-legacy")
    DriverManager.getConnection(jdbcUrl).use { connection ->
      createLegacySqliteSchema(connection)
      insertLegacyViolation(connection, createdAt = 1_765_033_573_944L)
    }

    createSqliteDataSource(jdbcUrl).use { dataSource ->
      createExecutor().migrate(dataSource, DatabaseType.SQLITE, announceCompat = false)
    }

    DriverManager.getConnection(jdbcUrl).use { connection ->
      assertTrue(columnExists(connection, "violations", "created_at_instant"))
      assertTrue(columnExists(connection, "monitor_settings", "show_name"))
      assertTrue(hasAppliedVersion(connection, "0.1"))
      assertTrue(hasAppliedVersion(connection, "1"))
      assertTrue(hasAppliedVersion(connection, "2"))
      assertTrue(hasAppliedVersion(connection, "3"))
    }
  }

  @Test
  fun `repairs the known transitional v1 checksum mismatch`() {
    val jdbcUrl = createJdbcUrl("slothac-executor-repair")
    migrateFreshSqlite(jdbcUrl)
    DriverManager.getConnection(jdbcUrl).use { connection ->
      connection.createStatement().use { statement ->
        statement.executeUpdate(
          "UPDATE flyway_schema_history SET checksum = $TRANSITIONAL_V1_CHECKSUM WHERE version = '1'"
        )
      }
    }

    val logger = mockk<Logger>(relaxed = true)
    val backupRequests = AtomicInteger()

    createSqliteDataSource(jdbcUrl).use { dataSource ->
      createExecutor(logger)
        .migrate(
          dataSource = dataSource,
          databaseType = DatabaseType.SQLITE,
          sqliteBackupSupplier = {
            backupRequests.incrementAndGet()
            null
          },
          announceCompat = false,
        )
    }

    DriverManager.getConnection(jdbcUrl).use { connection ->
      assertEquals(1, backupRequests.get())
      assertTrue(readChecksum(connection, "1") != TRANSITIONAL_V1_CHECKSUM)
    }
    verify(exactly = 1) {
      logger.warning(
        match<String> { message ->
          message.contains("Repairing Flyway schema history automatically.")
        }
      )
    }
  }

  private fun createExecutor(
    logger: Logger = Logger.getAnonymousLogger()
  ): DatabaseMigrationExecutor {
    val environment =
      DatabaseEnvironment(
        dataDirectory = Files.createTempDirectory("slothac-db-env"),
        logger = logger,
        classLoader = javaClass.classLoader,
      )
    return DatabaseMigrationExecutor(environment)
  }

  private fun createJdbcUrl(prefix: String): String {
    val databaseFile = Files.createTempFile(prefix, ".db").toFile()
    databaseFile.deleteOnExit()
    return "jdbc:sqlite:${databaseFile.absolutePath}"
  }

  private fun createSqliteDataSource(jdbcUrl: String): HikariDataSource {
    val config = HikariConfig()
    config.jdbcUrl = jdbcUrl
    config.poolName = "Sloth-Test-Pool"
    return HikariDataSource(config)
  }

  private fun migrateFreshSqlite(jdbcUrl: String) {
    Flyway.configure()
      .dataSource(jdbcUrl, null, null)
      .locations("classpath:db/migration/common", "classpath:db/migration/sqlite")
      .baselineVersion("0")
      .load()
      .migrate()
  }

  private fun createLegacySqliteSchema(connection: Connection) {
    connection.createStatement().use { statement ->
      statement.executeUpdate(
        """
        CREATE TABLE violations (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          server TEXT NOT NULL,
          uuid TEXT NOT NULL,
          player_name TEXT NOT NULL,
          check_name TEXT NOT NULL,
          verbose TEXT NOT NULL,
          vl INTEGER NOT NULL,
          created_at INTEGER NOT NULL
        )
        """
          .trimIndent()
      )
      statement.executeUpdate(
        """
        CREATE TABLE sloth_punishments (
          uuid TEXT NOT NULL,
          punish_group TEXT NOT NULL,
          vl INTEGER NOT NULL,
          PRIMARY KEY (uuid, punish_group)
        )
        """
          .trimIndent()
      )
      statement.executeUpdate(
        "CREATE INDEX idx_violations_uuid_time ON violations(uuid, created_at DESC)"
      )
      statement.executeUpdate("CREATE INDEX idx_violations_time ON violations(created_at DESC)")
      statement.executeUpdate(
        """
        CREATE TABLE monitor_settings (
          uuid TEXT NOT NULL PRIMARY KEY,
          mode TEXT NOT NULL,
          theme TEXT NOT NULL,
          show_ping INTEGER NOT NULL,
          show_dmg INTEGER NOT NULL,
          show_trend INTEGER NOT NULL
        )
        """
          .trimIndent()
      )
    }
  }

  private fun insertLegacyViolation(connection: Connection, createdAt: Long) {
    connection
      .prepareStatement(
        """
        INSERT INTO violations(server, uuid, player_name, check_name, verbose, vl, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """
          .trimIndent()
      )
      .use { statement ->
        statement.setString(1, "test")
        statement.setString(2, "00000000-0000-0000-0000-000000000001")
        statement.setString(3, "PlayerOne")
        statement.setString(4, "Aim")
        statement.setString(5, "legacy")
        statement.setInt(6, 12)
        statement.setLong(7, createdAt)
        statement.executeUpdate()
      }
  }

  private fun columnExists(connection: Connection, tableName: String, columnName: String): Boolean {
    connection.metaData.getColumns(null, null, tableName, columnName).use { resultSet ->
      return resultSet.next()
    }
  }

  private fun hasAppliedVersion(connection: Connection, version: String): Boolean {
    connection
      .prepareStatement(
        """
        SELECT 1
        FROM flyway_schema_history
        WHERE version = ? AND success = 1
        LIMIT 1
        """
          .trimIndent()
      )
      .use { statement ->
        statement.setString(1, version)
        statement.executeQuery().use { resultSet ->
          return resultSet.next()
        }
      }
  }

  private fun readChecksum(connection: Connection, version: String): Int? {
    connection
      .prepareStatement(
        """
        SELECT checksum
        FROM flyway_schema_history
        WHERE version = ?
        ORDER BY installed_rank DESC
        LIMIT 1
        """
          .trimIndent()
      )
      .use { statement ->
        statement.setString(1, version)
        statement.executeQuery().use { resultSet ->
          return if (resultSet.next()) resultSet.getInt("checksum") else null
        }
      }
  }
}
