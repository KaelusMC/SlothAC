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
    if (
      !"sqlite".equals(rawType, ignoreCase = true) && !"mysql".equals(rawType, ignoreCase = true)
    ) {
      plugin.logger.warning("Unknown database type '$rawType', defaulting to sqlite.")
    }

    dataSource = createDataSource(plugin, configManager, databaseType)
    exposedDb = Database.connect(dataSource)
    database = SqlViolationDatabase(plugin, configManager, exposedDb)
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

    if (databaseType == DatabaseType.MYSQL) {
      val host = configManager.config.getString("database.mysql.host", "localhost")
      val port = configManager.config.getInt("database.mysql.port", 3306)
      val database = configManager.config.getString("database.mysql.database", "slothac")
      val username = configManager.config.getString("database.mysql.username", "root")
      val password = configManager.config.getString("database.mysql.password", "")
      val useSsl = configManager.config.getBoolean("database.mysql.use-ssl", false)
      val allowPublicKeyRetrieval =
        configManager.config.getBoolean("database.mysql.allow-public-key-retrieval", false)

      val jdbcUrl =
        StringBuilder()
          .append("jdbc:mysql://")
          .append(host)
          .append(":")
          .append(port)
          .append("/")
          .append(database)
          .append("?useSSL=")
          .append(useSsl.toString().lowercase(Locale.ROOT))
          .append("&useUnicode=true&characterEncoding=utf8&serverTimezone=UTC")
      if (allowPublicKeyRetrieval) {
        jdbcUrl.append("&allowPublicKeyRetrieval=true")
      }

      config.driverClassName = "com.mysql.cj.jdbc.Driver"
      config.jdbcUrl = jdbcUrl.toString()
      config.username = username
      config.password = password

      config.addDataSourceProperty("cachePrepStmts", "true")
      config.addDataSourceProperty("prepStmtCacheSize", "250")
      config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    } else {
      val fileName = configManager.config.getString("database.sqlite.file", "violations.db")
      val dbFile = File(plugin.dataFolder, fileName)
      config.driverClassName = "org.sqlite.JDBC"
      config.jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
      config.addDataSourceProperty("journal_mode", "WAL")
      config.addDataSourceProperty("synchronous", "NORMAL")
      config.addDataSourceProperty("busy_timeout", "5000")
    }

    config.connectionTimeout = 30000
    config.idleTimeout = 600000
    config.maxLifetime = 1800000

    return HikariDataSource(config)
  }

  fun shutdown() {
    if (!dataSource.isClosed) {
      dataSource.close()
    }
  }
}
