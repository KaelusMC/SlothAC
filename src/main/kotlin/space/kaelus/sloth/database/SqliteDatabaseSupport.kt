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

import com.zaxxer.hikari.HikariDataSource
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.config.ConfigManager

private const val SQLITE_BACKUP_DIRECTORY = "db-backups"
private val SQLITE_BACKUP_TIMESTAMP: DateTimeFormatter =
  DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

internal fun resolveSqliteDatabaseFile(plugin: SlothAC, configManager: ConfigManager): Path {
  val fileName = configManager.config.getString("database.sqlite.file", "violations.db")
  return File(plugin.dataFolder, fileName).toPath()
}

internal fun createSqliteBackup(
  plugin: SlothAC,
  dataSource: HikariDataSource,
  databaseFile: Path,
): Path {
  val backupDirectory = plugin.dataFolder.toPath().resolve(SQLITE_BACKUP_DIRECTORY)
  Files.createDirectories(backupDirectory)
  val timestamp = LocalDateTime.now().format(SQLITE_BACKUP_TIMESTAMP)
  val backupFile =
    backupDirectory.resolve("${databaseFile.fileName.toString().removeSuffix(".db")}-$timestamp.db")

  dataSource.connection.use { connection ->
    val escapedPath = backupFile.toAbsolutePath().toString().replace("'", "''")
    connection.createStatement().use { statement ->
      statement.execute("VACUUM INTO '$escapedPath'")
    }
  }

  return backupFile
}

internal fun resetSqliteDatabaseFiles(databaseFile: Path) {
  listOf(
      databaseFile,
      sqliteSidecar(databaseFile, "-wal"),
      sqliteSidecar(databaseFile, "-shm"),
      sqliteSidecar(databaseFile, "-journal"),
    )
    .forEach(Files::deleteIfExists)
}

internal fun sqliteDatabaseHasContent(databaseFile: Path): Boolean {
  return Files.exists(databaseFile) && Files.size(databaseFile) > 0L
}

internal fun sqliteLegacyCompatRequired(connection: java.sql.Connection): Boolean {
  if (!sqliteTableExists(connection, "violations")) {
    return false
  }
  return !sqliteColumnExists(connection, "violations", "created_at_instant")
}

private fun sqliteSidecar(databaseFile: Path, suffix: String): Path {
  return databaseFile.resolveSibling(databaseFile.fileName.toString() + suffix)
}

private fun sqliteTableExists(connection: java.sql.Connection, tableName: String): Boolean {
  connection
    .prepareStatement("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1")
    .use { statement ->
      statement.setString(1, tableName)
      statement.executeQuery().use { resultSet ->
        return resultSet.next()
      }
    }
}

private fun sqliteColumnExists(
  connection: java.sql.Connection,
  tableName: String,
  columnName: String,
): Boolean {
  connection.metaData.getColumns(null, null, tableName, columnName).use { resultSet ->
    return resultSet.next()
  }
}
