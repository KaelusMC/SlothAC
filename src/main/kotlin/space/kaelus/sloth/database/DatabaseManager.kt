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
import java.util.logging.Level
import org.flywaydb.core.api.FlywayException
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.config.ConfigManager

class DatabaseManager(plugin: SlothAC, configManager: ConfigManager) {
  val database: ViolationDatabase
  private val dataSource: HikariDataSource?
  private val health = DatabaseHealth()

  init {
    val rawType = configManager.config.getString("database.type", "sqlite")
    val databaseType = DatabaseType.fromConfig(rawType)
    val environment =
      DatabaseEnvironment(
        dataDirectory = plugin.dataFolder.toPath(),
        logger = plugin.logger,
        classLoader = plugin.javaClass.classLoader,
      )
    val dataSourceFactory = DatabaseDataSourceFactory(environment, configManager)
    val migrationExecutor = DatabaseMigrationExecutor(environment)
    val sqliteRecovery = SqliteMigrationRecovery(environment, configManager, migrationExecutor)
    val fallbackDatabase = InMemoryViolationDatabase(configManager)
    if (SUPPORTED_DATABASE_TYPES.none { it.equals(rawType, ignoreCase = true) }) {
      environment.logger.warning(
        "Unknown database type $rawType, defaulting to sqlite. Supported types: sqlite, mysql, mariadb."
      )
    }

    val dataSourceResult = runCatching {
      createMigratedDataSource(
        databaseType = databaseType,
        dataSourceFactory = dataSourceFactory,
        migrationExecutor = migrationExecutor,
        sqliteRecovery = sqliteRecovery,
      )
    }

    if (dataSourceResult.isSuccess) {
      val resolvedDataSource = dataSourceResult.getOrThrow()
      dataSource = resolvedDataSource
      val persistentDatabase =
        SqlViolationDatabase(
          configManager = configManager,
          database = org.jetbrains.exposed.v1.jdbc.Database.connect(resolvedDataSource),
        )
      database =
        ResilientViolationDatabase(
          primary = persistentDatabase,
          fallback = fallbackDatabase,
          health = health,
          logger = plugin.logger,
        )
      health.markPersistent()
    } else {
      val failure = dataSourceResult.exceptionOrNull()!!
      environment.logger.log(
        Level.WARNING,
        buildString {
          append("Persistent database storage is unavailable")
          append(". Sloth will continue in degraded mode using in-memory storage")
          append(
            ". Data will work for the current runtime, but it will not persist across restart."
          )
        },
        failure,
      )
      dataSource = null
      database = fallbackDatabase
      health.markDegraded(failure)
    }
  }

  val isAvailable: Boolean
    get() = health.isPersistentAvailable()

  val failureCause: Throwable?
    get() = health.failureCause

  private fun createMigratedDataSource(
    databaseType: DatabaseType,
    dataSourceFactory: DatabaseDataSourceFactory,
    migrationExecutor: DatabaseMigrationExecutor,
    sqliteRecovery: SqliteMigrationRecovery,
  ): HikariDataSource {
    val initialDataSource = dataSourceFactory.create(databaseType)
    return runCatching {
        if (databaseType == DatabaseType.SQLITE) {
          sqliteRecovery.migrate(initialDataSource)
        } else {
          migrationExecutor.migrate(initialDataSource, databaseType)
          initialDataSource
        }
      }
      .getOrElse { exception ->
        closeQuietly(initialDataSource)
        if (exception is FlywayException) {
          throw IllegalStateException("Database migrations failed", exception)
        }
        throw exception
      }
  }

  fun shutdown() {
    val dataSource = dataSource ?: return
    if (!dataSource.isClosed) {
      dataSource.close()
    }
  }

  private fun closeQuietly(dataSource: HikariDataSource) {
    if (!dataSource.isClosed) {
      runCatching { dataSource.close() }
    }
  }
}
