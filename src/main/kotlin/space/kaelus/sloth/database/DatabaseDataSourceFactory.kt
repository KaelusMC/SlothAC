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
import java.util.Locale
import space.kaelus.sloth.config.ConfigManager

private const val MIN_POOL_SIZE = 2
private const val MAX_RECOMMENDED_POOL_SIZE = 8
private const val SQLITE_BUSY_TIMEOUT_MILLIS = 5000
private const val DEFAULT_MARIADB_PORT = 3306
private const val CONNECTION_TIMEOUT_MILLIS = 30000L
private const val IDLE_TIMEOUT_MILLIS = 600000L
private const val MAX_LIFETIME_MILLIS = 1800000L

internal class DatabaseDataSourceFactory(
  private val environment: DatabaseEnvironment,
  private val configManager: ConfigManager,
) {

  fun create(databaseType: DatabaseType): HikariDataSource {
    val config = HikariConfig()
    config.poolName = "Sloth-Pool"

    val defaultPoolSize =
      maxOf(
        MIN_POOL_SIZE,
        minOf(MAX_RECOMMENDED_POOL_SIZE, Runtime.getRuntime().availableProcessors()),
      )
    val poolSize = configManager.config.getInt("database.pool-size", defaultPoolSize)
    config.maximumPoolSize = maxOf(MIN_POOL_SIZE, poolSize)

    when (databaseType) {
      DatabaseType.SQLITE -> configureSqlite(config)
      DatabaseType.MARIADB -> configureMariaDb(config)
    }

    config.connectionTimeout = CONNECTION_TIMEOUT_MILLIS
    config.idleTimeout = IDLE_TIMEOUT_MILLIS
    config.maxLifetime = MAX_LIFETIME_MILLIS

    return HikariDataSource(config)
  }

  private fun configureSqlite(config: HikariConfig) {
    val dbFile = resolveSqliteDatabaseFile(environment.dataDirectory, configManager).toFile()
    config.jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
    config.addDataSourceProperty("journal_mode", "WAL")
    config.addDataSourceProperty("synchronous", "NORMAL")
    config.addDataSourceProperty("busy_timeout", SQLITE_BUSY_TIMEOUT_MILLIS.toString())
  }

  private fun configureMariaDb(config: HikariConfig) {
    val host = configManager.config.getString("database.mysql.host", "localhost")
    val port = configManager.config.getInt("database.mysql.port", DEFAULT_MARIADB_PORT)
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
}
