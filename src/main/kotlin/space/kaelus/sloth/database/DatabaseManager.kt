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
import java.io.File
import java.nio.file.Path
import java.util.Locale
import java.util.logging.Level
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.exception.FlywayValidateException
import org.jetbrains.exposed.v1.jdbc.Database
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.config.ConfigManager

class DatabaseManager(plugin: SlothAC, configManager: ConfigManager) {
  val database: ViolationDatabase
  val dataSource: HikariDataSource
  private val exposedDb: Database

  init {
    val rawType = configManager.config.getString("database.type", "sqlite")
    val databaseType = DatabaseType.fromConfig(rawType)
    if (SUPPORTED_DATABASE_TYPES.none { it.equals(rawType, ignoreCase = true) }) {
      plugin.logger.warning(
        "Unknown database type $rawType, defaulting to sqlite. Supported types: sqlite, mysql, mariadb."
      )
    }

    dataSource = createMigratedDataSource(plugin, configManager, databaseType)
    exposedDb = Database.connect(dataSource)
    database = SqlViolationDatabase(plugin, configManager, exposedDb)
  }

  private fun createMigratedDataSource(
    plugin: SlothAC,
    configManager: ConfigManager,
    databaseType: DatabaseType,
  ): HikariDataSource {
    val initialDataSource = createDataSource(plugin, configManager, databaseType)
    return if (databaseType == DatabaseType.SQLITE) {
      migrateSqliteWithRecovery(plugin, configManager, initialDataSource)
    } else {
      try {
        runFlywayMigrations(
          plugin = plugin,
          dataSource = initialDataSource,
          migrationFlyway = buildMigrationFlyway(plugin, initialDataSource, databaseType),
          databaseType = databaseType,
        )
      } catch (exception: FlywayException) {
        failMigrations(plugin, exception)
      }
      initialDataSource
    }
  }

  private fun migrateSqliteWithRecovery(
    plugin: SlothAC,
    configManager: ConfigManager,
    dataSource: HikariDataSource,
  ): HikariDataSource {
    val databaseFile = resolveSqliteDatabaseFile(plugin, configManager)
    var backupFile: Path? = null
    var migrationFlyway =
      buildMigrationFlyway(plugin, dataSource, DatabaseType.SQLITE, announceCompat = false)

    fun ensureBackup(): Path? =
      backupFile
        ?: takeIf { sqliteDatabaseHasContent(databaseFile) }
          ?.let {
            createSqliteBackup(plugin, dataSource, databaseFile).also { createdBackup ->
              backupFile = createdBackup
            }
          }

    val needsPreMigrationBackup =
      sqliteDatabaseHasContent(databaseFile) && migrationFlyway.info().pending().isNotEmpty()
    if (needsPreMigrationBackup) {
      ensureBackup()
    }

    return try {
      runFlywayMigrations(
        plugin = plugin,
        dataSource = dataSource,
        migrationFlyway = migrationFlyway,
        databaseType = DatabaseType.SQLITE,
        sqliteBackupSupplier = ::ensureBackup,
      )
      dataSource
    } catch (exception: FlywayException) {
      val preservedBackup = ensureBackup()
      plugin.logger.warning(
        buildString {
          append("SQLite migrations failed")
          preservedBackup?.let { append(". Preserved the previous database at $it") }
          append(". Recreating a fresh SQLite database so the plugin can still start.")
        }
      )
      if (!dataSource.isClosed) {
        dataSource.close()
      }
      resetSqliteDatabaseFiles(databaseFile)

      val freshDataSource = createDataSource(plugin, configManager, DatabaseType.SQLITE)
      try {
        migrationFlyway = buildMigrationFlyway(plugin, freshDataSource, DatabaseType.SQLITE)
        runFlywayMigrations(
          plugin = plugin,
          dataSource = freshDataSource,
          migrationFlyway = migrationFlyway,
          databaseType = DatabaseType.SQLITE,
        )
        plugin.logger.warning(
          buildString {
            append("Sloth started with a fresh SQLite database after migration recovery")
            preservedBackup?.let { append(". Previous data is available at $it") }
            append(".")
          }
        )
        freshDataSource
      } catch (retryException: FlywayException) {
        retryException.addSuppressed(exception)
        if (!freshDataSource.isClosed) {
          freshDataSource.close()
        }
        failMigrations(plugin, retryException)
      }
    }
  }

  private fun runFlywayMigrations(
    plugin: SlothAC,
    dataSource: HikariDataSource,
    migrationFlyway: Flyway,
    databaseType: DatabaseType,
    sqliteBackupSupplier: (() -> Path?)? = null,
  ) {
    var activeFlyway = migrationFlyway
    val validation = activeFlyway.validateWithResult()
    if (!validation.validationSuccessful) {
      if (
        databaseType == DatabaseType.SQLITE &&
          isRepairableSqliteV1ChecksumMismatch(validation, dataSource)
      ) {
        val backupPath = sqliteBackupSupplier?.invoke()
        plugin.logger.warning(
          buildString {
            append("Detected a legacy SQLite checksum mismatch for migration V1")
            backupPath?.let { append(". Preserved the previous database at $it") }
            append(". Repairing Flyway schema history automatically.")
          }
        )
        activeFlyway.repair()
        activeFlyway = buildMigrationFlyway(plugin, dataSource, databaseType)
        val repairedValidation = activeFlyway.validateWithResult()
        if (!repairedValidation.validationSuccessful) {
          throw FlywayValidateException(
            repairedValidation.errorDetails,
            repairedValidation.getAllErrorMessages(),
          )
        }
      } else {
        throw FlywayValidateException(validation.errorDetails, validation.getAllErrorMessages())
      }
    }

    activeFlyway.migrate()
  }

  private fun failMigrations(plugin: SlothAC, exception: FlywayException): Nothing {
    plugin.logger.log(Level.SEVERE, "Failed to run database migrations", exception)
    throw IllegalStateException("Database migrations failed", exception)
  }

  private fun createDataSource(
    plugin: SlothAC,
    configManager: ConfigManager,
    databaseType: DatabaseType,
  ): HikariDataSource {
    val config = HikariConfig()
    config.poolName = "Sloth-Pool"

    val defaultPoolSize = maxOf(2, minOf(8, Runtime.getRuntime().availableProcessors()))
    val poolSize = configManager.config.getInt("database.pool-size", defaultPoolSize)
    config.maximumPoolSize = maxOf(2, poolSize)

    if (databaseType == DatabaseType.SQLITE) {
      val fileName = configManager.config.getString("database.sqlite.file", "violations.db")
      val dbFile = File(plugin.dataFolder, fileName)
      config.jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
      config.addDataSourceProperty("journal_mode", "WAL")
      config.addDataSourceProperty("synchronous", "NORMAL")
      config.addDataSourceProperty("busy_timeout", "5000")
    } else {
      val host = configManager.config.getString("database.mysql.host", "localhost")
      val port = configManager.config.getInt("database.mysql.port", 3306)
      val database = configManager.config.getString("database.mysql.database", "slothac")
      val username = configManager.config.getString("database.mysql.username", "root")
      val password = configManager.config.getString("database.mysql.password", "")
      val useSsl = configManager.config.getBoolean("database.mysql.use-ssl", false)

      config.driverClassName = MARIADB_DRIVER_CLASS
      config.jdbcUrl =
        buildMariaDbJdbcUrl(host = host, port = port, database = database, useSsl = useSsl)
      config.username = username
      config.password = password
    }

    config.connectionTimeout = 30000
    config.idleTimeout = 600000
    config.maxLifetime = 1800000

    return HikariDataSource(config)
  }

  private fun buildMariaDbJdbcUrl(
    host: String,
    port: Int,
    database: String,
    useSsl: Boolean,
  ): String {
    return StringBuilder()
      .append(MARIADB_JDBC_SCHEME)
      .append(host)
      .append(":")
      .append(port)
      .append("/")
      .append(database)
      .append("?useSsl=")
      .append(useSsl.toString().lowercase(Locale.ROOT))
      .toString()
  }

  fun shutdown() {
    if (!dataSource.isClosed) {
      dataSource.close()
    }
  }
}
