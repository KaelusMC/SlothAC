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
import java.util.Locale
import java.util.logging.Level
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
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

    dataSource = createDataSource(plugin, configManager, databaseType)
    runFlywayMigrations(plugin, dataSource, databaseType)
    exposedDb = Database.connect(dataSource)
    database = SqlViolationDatabase(plugin, configManager, exposedDb)
  }

  private fun runFlywayMigrations(
    plugin: SlothAC,
    dataSource: HikariDataSource,
    databaseType: DatabaseType,
  ) {
    val defaultLocations = defaultFlywayLocations(databaseType)
    val defaultFlyway = buildFlyway(plugin, dataSource, defaultLocations)
    val migrationFlyway =
      if (
        databaseType == DatabaseType.SQLITE &&
          hasAppliedMigrationVersion(defaultFlyway, LEGACY_SQLITE_COMPAT_VERSION)
      ) {
        plugin.logger.info(LEGACY_SQLITE_COMPAT_LOG)
        buildFlyway(plugin, dataSource, defaultLocations + LEGACY_SQLITE_COMPAT_LOCATION)
      } else {
        defaultFlyway
      }

    try {
      migrationFlyway.migrate()
    } catch (exception: FlywayException) {
      failMigrations(plugin, exception)
    }
  }

  private fun defaultFlywayLocations(databaseType: DatabaseType): List<String> {
    return listOf(
      COMMON_MIGRATION_LOCATION,
      "classpath:db/migration/${databaseType.flywayLocation}",
    )
  }

  private fun buildFlyway(
    plugin: SlothAC,
    dataSource: HikariDataSource,
    locations: List<String>,
  ): Flyway {
    require(locations.size == DEFAULT_LOCATIONS_COUNT || locations.size == COMPAT_LOCATIONS_COUNT) {
      "Unexpected Flyway locations count: ${locations.size}"
    }
    val configuration =
      Flyway.configure(plugin.javaClass.classLoader)
        .dataSource(dataSource)
        .baselineOnMigrate(true)
        .baselineVersion("0")
    if (locations.size == DEFAULT_LOCATIONS_COUNT) {
      configuration.locations(locations[0], locations[1])
    } else {
      configuration.locations(locations[0], locations[1], locations[2])
    }
    return configuration.load()
  }

  private fun hasAppliedMigrationVersion(flyway: Flyway, version: String): Boolean {
    return flyway.info().all().any { migration ->
      val migrationVersion = migration.version?.version
      migrationVersion == version && migration.installedRank != null
    }
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

  private companion object {
    val SUPPORTED_DATABASE_TYPES = setOf("sqlite", "mysql", "mariadb")
    const val COMMON_MIGRATION_LOCATION = "classpath:db/migration/common"
    const val DEFAULT_LOCATIONS_COUNT = 2
    const val COMPAT_LOCATIONS_COUNT = 3
    const val LEGACY_SQLITE_COMPAT_VERSION = "0.1"
    const val LEGACY_SQLITE_COMPAT_LOCATION = "classpath:db/migration/sqlitecompat"
    const val MARIADB_DRIVER_CLASS = "org.mariadb.jdbc.Driver"
    const val MARIADB_JDBC_SCHEME = "jdbc:mariadb://"
    const val LEGACY_SQLITE_COMPAT_LOG =
      "Detected legacy Flyway migration 0.1 in schema history. Enabling compatibility location for validation."
  }
}
