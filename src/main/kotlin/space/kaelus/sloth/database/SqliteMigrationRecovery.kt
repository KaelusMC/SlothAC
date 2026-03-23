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
import java.nio.file.Path
import java.util.logging.Level
import org.flywaydb.core.api.FlywayException
import space.kaelus.sloth.config.ConfigManager

internal class SqliteMigrationRecovery(
  private val environment: DatabaseEnvironment,
  private val configManager: ConfigManager,
  private val migrationExecutor: DatabaseMigrationExecutor,
) {

  fun migrate(initialDataSource: HikariDataSource): HikariDataSource {
    val databaseFile = resolveSqliteDatabaseFile(environment.dataDirectory, configManager)
    var backupFile: Path? = null

    fun ensureBackup(): Path? {
      if (backupFile != null || !sqliteDatabaseHasContent(databaseFile)) {
        return backupFile
      }

      return runCatching {
          createSqliteBackup(environment.dataDirectory, initialDataSource, databaseFile)
        }
        .onSuccess { createdBackup -> backupFile = createdBackup }
        .onFailure { exception ->
          environment.logger.log(
            Level.WARNING,
            "Failed to create a SQLite backup before migration recovery.",
            exception,
          )
        }
        .getOrNull()
    }

    val needsPreMigrationBackup =
      sqliteDatabaseHasContent(databaseFile) &&
        runCatching {
            migrationExecutor.hasPendingMigrations(
              dataSource = initialDataSource,
              databaseType = DatabaseType.SQLITE,
              announceCompat = false,
            )
          }
          .getOrDefault(false)
    if (needsPreMigrationBackup) {
      ensureBackup()
    }

    return try {
      migrationExecutor.migrate(
        dataSource = initialDataSource,
        databaseType = DatabaseType.SQLITE,
        sqliteBackupSupplier = ::ensureBackup,
        announceCompat = false,
      )
      initialDataSource
    } catch (exception: FlywayException) {
      logMigrationFailure(ensureBackup())
      throw exception
    }
  }

  private fun logMigrationFailure(preservedBackup: Path?) {
    environment.logger.warning(
      buildString {
        append("SQLite migrations failed")
        preservedBackup?.let { append(". Preserved the previous database at $it") }
        append(". Sloth will switch to in-memory storage until the database issue is fixed.")
      }
    )
  }
}
