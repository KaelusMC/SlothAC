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
@file:Suppress("ClassName")

package db.migration.sqlitecompat

import java.sql.Connection
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V0_1__LegacySchemaPatch : BaseJavaMigration() {
  override fun migrate(context: Context) {
    val connection = context.connection
    if (!tableExists(connection, "violations")) {
      return
    }
    if (columnExists(connection, "violations", "created_at_instant")) {
      return
    }

    connection.createStatement().use { statement ->
      statement.executeUpdate(
        "ALTER TABLE violations ADD COLUMN created_at_instant TEXT NOT NULL DEFAULT '1970-01-01 00:00:00.000'"
      )
    }
  }

  private fun tableExists(connection: Connection, tableName: String): Boolean {
    connection
      .prepareStatement("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1")
      .use { statement ->
        statement.setString(1, tableName)
        statement.executeQuery().use { resultSet ->
          return resultSet.next()
        }
      }
  }

  private fun columnExists(connection: Connection, tableName: String, columnName: String): Boolean {
    connection.metaData.getColumns(null, null, tableName, columnName).use { resultSet ->
      return resultSet.next()
    }
  }
}
