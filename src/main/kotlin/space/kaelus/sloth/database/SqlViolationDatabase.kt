/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2026 KaelusMC
 *
 * This file contains code derived from GrimAC.
 * The original authors of GrimAC are credited below.
 *
 * Copyright (c) 2021-2026 GrimAC, DefineOutside and contributors.
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

import java.sql.SQLException
import java.time.Instant
import java.util.UUID
import java.util.logging.Level
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.JavaInstantColumnType
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.monitor.MonitorMode
import space.kaelus.sloth.monitor.MonitorNameMode
import space.kaelus.sloth.monitor.MonitorSettings
import space.kaelus.sloth.monitor.MonitorTheme
import space.kaelus.sloth.player.SlothPlayer

class SqlViolationDatabase(
  private val plugin: SlothAC,
  private val configManager: ConfigManager,
  private val database: Database,
) : ViolationDatabase {

  override fun logAlert(player: SlothPlayer, verbose: String, checkName: String, vls: Int) {
    try {
      transaction(database) {
        val now = Instant.now()
        Violations.insert {
          it[server] = configManager.config.getString("history.server-name", "server")
          it[uuid] = player.uuid.toString()
          it[playerName] = player.player.name
          it[Violations.checkName] = checkName
          it[Violations.verbose] = verbose
          it[vl] = vls
          it[createdAt] = now.toEpochMilli()
          it[createdAtInstant] = now
        }
      }
    } catch (e: SQLException) {
      plugin.logger.log(Level.SEVERE, "Failed to log violation", e)
    }
  }

  override fun getLogCount(player: UUID): Int {
    return try {
      transaction(database) {
        Violations.selectAll().where { Violations.uuid eq player.toString() }.count().toInt()
      }
    } catch (e: SQLException) {
      plugin.logger.log(Level.SEVERE, "Failed to count violations", e)
      0
    }
  }

  override fun getViolations(player: UUID, page: Int, limit: Int): List<Violation> {
    return try {
      transaction(database) {
        Violations.selectAll()
          .where { Violations.uuid eq player.toString() }
          .orderBy(Violations.createdAtInstant to SortOrder.DESC)
          .limit(limit)
          .offset(((page - 1) * limit).toLong())
          .map(::toViolation)
      }
    } catch (e: SQLException) {
      plugin.logger.log(Level.SEVERE, "Failed to get violations", e)
      emptyList()
    }
  }

  override fun getLogCount(since: Long): Int {
    return try {
      transaction(database) {
        val sinceInstant = Instant.ofEpochMilli(since)
        val query =
          if (since > 0) {
            Violations.selectAll().where { Violations.createdAtInstant greaterEq sinceInstant }
          } else {
            Violations.selectAll()
          }
        query.count().toInt()
      }
    } catch (e: SQLException) {
      plugin.logger.log(Level.SEVERE, "Failed to count all violations", e)
      0
    }
  }

  override fun getViolations(page: Int, limit: Int, since: Long): List<Violation> {
    return try {
      transaction(database) {
        val sinceInstant = Instant.ofEpochMilli(since)
        val query =
          if (since > 0) {
            Violations.selectAll().where { Violations.createdAtInstant greaterEq sinceInstant }
          } else {
            Violations.selectAll()
          }
        query
          .orderBy(Violations.createdAtInstant to SortOrder.DESC)
          .limit(limit)
          .offset(((page - 1) * limit).toLong())
          .map(::toViolation)
      }
    } catch (e: SQLException) {
      plugin.logger.log(Level.SEVERE, "Failed to get all violations", e)
      emptyList()
    }
  }

  override fun getViolationLevel(playerUUID: UUID, punishGroupName: String): Int {
    return try {
      transaction(database) {
        Punishments.selectAll()
          .where {
            (Punishments.uuid eq playerUUID.toString()) and
              (Punishments.punishGroup eq punishGroupName)
          }
          .firstOrNull()
          ?.get(Punishments.vl) ?: 0
      }
    } catch (e: SQLException) {
      plugin.logger.log(Level.SEVERE, "Failed to get violation level for $playerUUID", e)
      0
    }
  }

  override fun incrementViolationLevel(playerUUID: UUID, punishGroupName: String): Int {
    return try {
      transaction(database) {
        Punishments.upsert(
          Punishments.uuid,
          Punishments.punishGroup,
          onUpdate = { it[Punishments.vl] = Punishments.vl + 1 },
        ) {
          it[uuid] = playerUUID.toString()
          it[punishGroup] = punishGroupName
          it[vl] = 1
        }

        Punishments.selectAll()
          .where {
            (Punishments.uuid eq playerUUID.toString()) and
              (Punishments.punishGroup eq punishGroupName)
          }
          .firstOrNull()
          ?.get(Punishments.vl) ?: 0
      }
    } catch (e: SQLException) {
      plugin.logger.log(Level.SEVERE, "Failed to increment violation level for $playerUUID", e)
      0
    }
  }

  override fun getUniqueViolatorsSince(since: Long): Int {
    return try {
      transaction(database) {
        val sinceInstant = Instant.ofEpochMilli(since)
        Violations.select(Violations.uuid.countDistinct())
          .where { Violations.createdAtInstant greaterEq sinceInstant }
          .firstOrNull()
          ?.get(Violations.uuid.countDistinct())
          ?.toInt() ?: 0
      }
    } catch (e: SQLException) {
      plugin.logger.log(Level.SEVERE, "Failed to count unique violators", e)
      0
    }
  }

  override fun resetViolationLevel(playerUUID: UUID, punishGroupName: String) {
    try {
      transaction(database) {
        Punishments.deleteWhere {
          (Punishments.uuid eq playerUUID.toString()) and
            (Punishments.punishGroup eq punishGroupName)
        }
      }
    } catch (e: SQLException) {
      plugin.logger.log(Level.SEVERE, "Failed to reset violation level for $playerUUID", e)
    }
  }

  override fun resetAllViolationLevels(playerUUID: UUID) {
    try {
      transaction(database) {
        Punishments.deleteWhere { Punishments.uuid eq playerUUID.toString() }
      }
    } catch (e: SQLException) {
      plugin.logger.log(Level.SEVERE, "Failed to reset all violation levels for $playerUUID", e)
    }
  }

  override fun loadMonitorSettings(playerUUID: UUID): MonitorSettings? {
    return try {
      transaction(database) {
        MonitorSettingsTable.selectAll()
          .where { MonitorSettingsTable.uuid eq playerUUID.toString() }
          .firstOrNull()
          ?.let { row ->
            val mode = MonitorMode.fromConfig(row[MonitorSettingsTable.mode])
            val theme = MonitorTheme.fromConfig(row[MonitorSettingsTable.theme])
            val showName = MonitorNameMode.fromConfig(row[MonitorSettingsTable.showName])
            MonitorSettings(
              mode,
              theme,
              row[MonitorSettingsTable.showPing],
              row[MonitorSettingsTable.showDmg],
              row[MonitorSettingsTable.showTrend],
              showName,
            )
          }
      }
    } catch (e: SQLException) {
      plugin.logger.log(Level.SEVERE, "Failed to load monitor settings", e)
      null
    }
  }

  override fun saveMonitorSettings(playerUUID: UUID, settings: MonitorSettings) {
    try {
      transaction(database) {
        MonitorSettingsTable.upsert(
          MonitorSettingsTable.uuid,
          onUpdate = {
            it[MonitorSettingsTable.mode] = insertValue(MonitorSettingsTable.mode)
            it[MonitorSettingsTable.theme] = insertValue(MonitorSettingsTable.theme)
            it[MonitorSettingsTable.showPing] = insertValue(MonitorSettingsTable.showPing)
            it[MonitorSettingsTable.showDmg] = insertValue(MonitorSettingsTable.showDmg)
            it[MonitorSettingsTable.showTrend] = insertValue(MonitorSettingsTable.showTrend)
            it[MonitorSettingsTable.showName] = insertValue(MonitorSettingsTable.showName)
          },
        ) {
          it[uuid] = playerUUID.toString()
          it[mode] = settings.mode.name
          it[theme] = settings.theme.name
          it[showPing] = settings.showPing
          it[showDmg] = settings.showDmg
          it[showTrend] = settings.showTrend
          it[showName] = settings.showName.name
        }
      }
    } catch (e: SQLException) {
      plugin.logger.log(Level.SEVERE, "Failed to save monitor settings", e)
    }
  }

  private fun toViolation(row: ResultRow): Violation {
    val instant = row[Violations.createdAtInstant]
    return Violation(
      serverName = row[Violations.server],
      playerUUID = UUID.fromString(row[Violations.uuid]),
      playerName = row[Violations.playerName],
      checkName = row[Violations.checkName],
      verbose = row[Violations.verbose],
      vl = row[Violations.vl],
      createdAt =
        if (instant == Instant.EPOCH) {
          Instant.ofEpochMilli(row[Violations.createdAt])
        } else {
          instant
        },
    )
  }

  private object Violations : Table("violations") {
    val id: Column<Long> = long("id").autoIncrement()
    val server: Column<String> = varchar("server", 255)
    val uuid: Column<String> = varchar("uuid", 36)
    val playerName: Column<String> = varchar("player_name", 64)
    val checkName: Column<String> = varchar("check_name", 255)
    val verbose: Column<String> = text("verbose")
    val vl: Column<Int> = integer("vl")
    val createdAt: Column<Long> = long("created_at")
    val createdAtInstant: Column<Instant> =
      registerColumn("created_at_instant", JavaInstantColumnType()).default(Instant.EPOCH)

    override val primaryKey = PrimaryKey(id)

    init {
      index(isUnique = false, uuid, createdAt)
      index(isUnique = false, createdAt)
      index(isUnique = false, uuid, createdAtInstant)
      index(isUnique = false, createdAtInstant)
    }
  }

  private object Punishments : Table("sloth_punishments") {
    val uuid: Column<String> = varchar("uuid", 36)
    val punishGroup: Column<String> = varchar("punish_group", 255)
    val vl: Column<Int> = integer("vl")

    override val primaryKey = PrimaryKey(uuid, punishGroup)
  }

  private object MonitorSettingsTable : Table("monitor_settings") {
    val uuid: Column<String> = varchar("uuid", 36)
    val mode: Column<String> = varchar("mode", 16)
    val theme: Column<String> = varchar("theme", 16)
    val showPing: Column<Boolean> = bool("show_ping")
    val showDmg: Column<Boolean> = bool("show_dmg")
    val showTrend: Column<Boolean> = bool("show_trend")
    val showName: Column<String> = varchar("show_name", 16)

    override val primaryKey = PrimaryKey(uuid)
  }
}
