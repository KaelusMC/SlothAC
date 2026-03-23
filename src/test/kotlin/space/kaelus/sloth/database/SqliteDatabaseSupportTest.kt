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
import java.sql.DriverManager
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class SqliteDatabaseSupportTest {

  @Test
  fun `requires explicit baseline only for recognized legacy sloth schemas`() {
    val jdbcUrl = createJdbcUrl("slothac-sqlite-baseline")

    DriverManager.getConnection(jdbcUrl).use { connection ->
      assertFalse(sqliteRequiresExplicitBaseline(connection))

      connection.createStatement().use { statement ->
        statement.executeUpdate("CREATE TABLE unrelated(id INTEGER PRIMARY KEY)")
      }
      assertFalse(sqliteRequiresExplicitBaseline(connection))

      connection.createStatement().use { statement ->
        statement.executeUpdate(
          "CREATE TABLE violations(id INTEGER PRIMARY KEY, created_at INTEGER NOT NULL)"
        )
      }
      assertTrue(sqliteRequiresExplicitBaseline(connection))
    }
  }

  @Test
  fun `detects when sqlite legacy compatibility migrations are required`() {
    val jdbcUrl = createJdbcUrl("slothac-sqlite-compat")

    DriverManager.getConnection(jdbcUrl).use { connection ->
      connection.createStatement().use { statement ->
        statement.executeUpdate(
          """
          CREATE TABLE violations(
            id INTEGER PRIMARY KEY,
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
      }

      assertTrue(sqliteLegacyCompatRequired(connection))

      connection.createStatement().use { statement ->
        statement.executeUpdate(
          "ALTER TABLE violations ADD COLUMN created_at_instant TEXT NOT NULL DEFAULT '1970-01-01 00:00:00.000'"
        )
      }

      assertFalse(sqliteLegacyCompatRequired(connection))
    }
  }

  private fun createJdbcUrl(prefix: String): String {
    val databaseFile = Files.createTempFile(prefix, ".db").toFile()
    databaseFile.deleteOnExit()
    return "jdbc:sqlite:${databaseFile.absolutePath}"
  }
}
