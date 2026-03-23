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
import org.flywaydb.core.api.exception.FlywayValidateException

private const val SQLITE_BASELINE_LOG =
  "Detected a legacy SQLite schema without Flyway history. Baselining it explicitly before migrations."

internal class DatabaseMigrationExecutor(private val environment: DatabaseEnvironment) {

  fun migrate(
    dataSource: HikariDataSource,
    databaseType: DatabaseType,
    sqliteBackupSupplier: (() -> Path?)? = null,
    announceCompat: Boolean = true,
  ) {
    var activeFlyway =
      buildMigrationFlyway(
        environment.classLoader,
        environment.logger,
        dataSource,
        databaseType,
        announceCompat,
      )
    if (requiresExplicitBaseline(dataSource, databaseType)) {
      environment.logger.info(SQLITE_BASELINE_LOG)
      activeFlyway.baseline()
      activeFlyway =
        buildMigrationFlyway(
          environment.classLoader,
          environment.logger,
          dataSource,
          databaseType,
          announceCompat,
        )
    }

    try {
      activeFlyway.migrate()
    } catch (exception: FlywayValidateException) {
      val validation = activeFlyway.validateWithResult()
      if (
        databaseType == DatabaseType.SQLITE &&
          isRepairableSqliteV1ChecksumMismatch(validation, dataSource)
      ) {
        val backupPath = sqliteBackupSupplier?.invoke()
        environment.logger.warning(
          buildString {
            append("Detected a legacy SQLite checksum mismatch for migration V1")
            backupPath?.let { append(". Preserved the previous database at $it") }
            append(". Repairing Flyway schema history automatically.")
          }
        )
        activeFlyway.repair()
        activeFlyway =
          buildMigrationFlyway(
            environment.classLoader,
            environment.logger,
            dataSource,
            databaseType,
          )
        activeFlyway.migrate()
      } else {
        throw exception
      }
    }
  }
}
