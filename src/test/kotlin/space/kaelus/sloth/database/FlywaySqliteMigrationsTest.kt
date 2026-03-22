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

import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.assertTrue
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test

class FlywaySqliteMigrationsTest {

  @Test
  fun `applies sqlite sql and java migrations`() {
    val databaseFile = Files.createTempFile("slothac-flyway-", ".db").toFile()
    databaseFile.deleteOnExit()
    val jdbcUrl = "jdbc:sqlite:${databaseFile.absolutePath}"

    Flyway.configure()
      .dataSource(jdbcUrl, null, null)
      .locations("classpath:db/migration/common", "classpath:db/migration/sqlite")
      .baselineOnMigrate(true)
      .baselineVersion("0")
      .load()
      .migrate()

    DriverManager.getConnection(jdbcUrl).use { connection ->
      assertTrue(tableExists(connection, "violations"))
      assertTrue(tableExists(connection, "sloth_punishments"))
      assertTrue(tableExists(connection, "monitor_settings"))
      assertTrue(columnExists(connection, "violations", "created_at_instant"))
      assertTrue(columnExists(connection, "monitor_settings", "show_name"))
      val versions = appliedVersions(connection)
      assertTrue(versions.contains("1"))
      assertTrue(versions.contains("2"))
      assertTrue(versions.contains("3"))
    }
  }

  private fun tableExists(connection: Connection, tableName: String): Boolean {
    connection.metaData.getTables(null, null, tableName, arrayOf("TABLE")).use { resultSet ->
      return resultSet.next()
    }
  }

  private fun columnExists(connection: Connection, tableName: String, columnName: String): Boolean {
    connection.metaData.getColumns(null, null, tableName, columnName).use { resultSet ->
      return resultSet.next()
    }
  }

  private fun appliedVersions(connection: Connection): Set<String> {
    val versions = mutableSetOf<String>()
    connection.createStatement().use { statement ->
      statement
        .executeQuery(
          "SELECT version FROM flyway_schema_history WHERE success = 1 AND version IS NOT NULL"
        )
        .use { resultSet ->
          while (resultSet.next()) {
            resultSet.getString("version")?.takeIf { it.isNotBlank() }?.let(versions::add)
          }
        }
    }
    return versions
  }
}
