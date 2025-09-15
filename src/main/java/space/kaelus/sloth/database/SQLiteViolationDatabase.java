/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2025 KaelusMC
 *
 * This file contains code derived from GrimAC.
 * The original authors of GrimAC are credited below.
 *
 * Copyright (c) 2021-2025 GrimAC, DefineOutside and contributors.
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
package space.kaelus.sloth.database;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.config.ConfigManager;
import space.kaelus.sloth.player.SlothPlayer;

public class SQLiteViolationDatabase implements ViolationDatabase {
  private final HikariDataSource dataSource;
  private final SlothAC plugin;
  private final ConfigManager configManager;

  public SQLiteViolationDatabase(
      HikariDataSource dataSource, SlothAC plugin, ConfigManager configManager) {
    this.dataSource = dataSource;
    this.plugin = plugin;
    this.configManager = configManager;
    initTables();
  }

  private void initTables() {
    try (Connection conn = dataSource.getConnection();
        Statement statement = conn.createStatement()) {

      statement.execute(
          "CREATE TABLE IF NOT EXISTS violations("
              + "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, "
              + "server VARCHAR(255) NOT NULL, "
              + "uuid CHAR(36) NOT NULL, "
              + "player_name TEXT NOT NULL, "
              + "check_name TEXT NOT NULL, "
              + "verbose TEXT NOT NULL, "
              + "vl INTEGER NOT NULL, "
              + "created_at BIGINT NOT NULL);");

      statement.execute("DROP INDEX IF EXISTS idx_violations_uuid;");
      statement.execute(
          "CREATE INDEX IF NOT EXISTS idx_violations_uuid_time ON violations(uuid, created_at DESC);");
      statement.execute(
          "CREATE INDEX IF NOT EXISTS idx_violations_time ON violations(created_at DESC);");

      statement.execute(
          "CREATE TABLE IF NOT EXISTS sloth_punishments ("
              + "uuid CHAR(36) NOT NULL, "
              + "punish_group VARCHAR(255) NOT NULL, "
              + "vl INTEGER NOT NULL, "
              + "PRIMARY KEY (uuid, punish_group));");

    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to initialize database tables", e);
    }
  }

  @Override
  public void logAlert(SlothPlayer player, String verbose, String checkName, int vls) {
    String sql =
        "INSERT INTO violations (server, uuid, player_name, check_name, verbose, vl, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, configManager.getConfig().getString("history.server-name", "server"));
      ps.setString(2, player.getUuid().toString());
      ps.setString(3, player.getPlayer().getName());
      ps.setString(4, checkName);
      ps.setString(5, verbose);
      ps.setInt(6, vls);
      ps.setLong(7, System.currentTimeMillis());
      ps.executeUpdate();
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to log violation", e);
    }
  }

  @Override
  public int getLogCount(UUID player) {
    String sql = "SELECT COUNT(*) FROM violations WHERE uuid = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, player.toString());
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to count violations", e);
    }
    return 0;
  }

  @Override
  public List<Violation> getViolations(UUID player, int page, int limit) {
    String sql =
        "SELECT * FROM violations WHERE uuid = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, player.toString());
      ps.setInt(2, limit);
      ps.setInt(3, (page - 1) * limit);
      try (ResultSet rs = ps.executeQuery()) {
        return Violation.fromResultSet(rs);
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to get violations", e);
    }
    return List.of();
  }

  @Override
  public int getLogCount(long since) {
    String sql = "SELECT COUNT(*) FROM violations" + (since > 0 ? " WHERE created_at >= ?" : "");
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      if (since > 0) {
        ps.setLong(1, since);
      }
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to count all violations", e);
    }
    return 0;
  }

  @Override
  public List<Violation> getViolations(int page, int limit, long since) {
    String sql =
        "SELECT * FROM violations"
            + (since > 0 ? " WHERE created_at >= ?" : "")
            + " ORDER BY created_at DESC LIMIT ? OFFSET ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      int paramIndex = 1;
      if (since > 0) {
        ps.setLong(paramIndex++, since);
      }
      ps.setInt(paramIndex++, limit);
      ps.setInt(paramIndex, (page - 1) * limit);
      try (ResultSet rs = ps.executeQuery()) {
        return Violation.fromResultSet(rs);
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to get all violations", e);
    }
    return List.of();
  }

  @Override
  public int getViolationLevel(UUID playerUUID, String punishGroupName) {
    String sql = "SELECT vl FROM sloth_punishments WHERE uuid = ? AND punish_group = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, playerUUID.toString());
      ps.setString(2, punishGroupName);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("vl");
        }
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to get violation level for " + playerUUID, e);
    }
    return 0;
  }

  @Override
  public int incrementViolationLevel(UUID playerUUID, String punishGroupName) {
    String upsertSQL =
        "INSERT INTO sloth_punishments (uuid, punish_group, vl) VALUES (?, ?, 1) "
            + "ON CONFLICT(uuid, punish_group) DO UPDATE SET vl = vl + 1";
    String selectSQL = "SELECT vl FROM sloth_punishments WHERE uuid = ? AND punish_group = ?";

    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);

      try {
        try (PreparedStatement psUpsert = conn.prepareStatement(upsertSQL)) {
          psUpsert.setString(1, playerUUID.toString());
          psUpsert.setString(2, punishGroupName);
          psUpsert.executeUpdate();
        }

        try (PreparedStatement psSelect = conn.prepareStatement(selectSQL)) {
          psSelect.setString(1, playerUUID.toString());
          psSelect.setString(2, punishGroupName);
          try (ResultSet rs = psSelect.executeQuery()) {
            if (rs.next()) {
              int newVl = rs.getInt(1);
              conn.commit();
              return newVl;
            }
          }
        }

        conn.rollback();
        return 0;

      } catch (SQLException e) {
        conn.rollback();
        plugin
            .getLogger()
            .log(Level.SEVERE, "Failed to increment violation level for " + playerUUID, e);
        return 0;
      }
    } catch (SQLException e) {
      plugin
          .getLogger()
          .log(
              Level.SEVERE,
              "Database connection error while incrementing violation level for " + playerUUID,
              e);
      return 0;
    }
  }

  @Override
  public int getUniqueViolatorsSince(long since) {
    String sql = "SELECT COUNT(DISTINCT uuid) FROM violations WHERE created_at >= ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, since);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to count unique violators", e);
    }
    return 0;
  }

  @Override
  public void resetViolationLevel(UUID playerUUID, String punishGroupName) {
    String sql = "DELETE FROM sloth_punishments WHERE uuid = ? AND punish_group = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, playerUUID.toString());
      ps.setString(2, punishGroupName);
      ps.executeUpdate();
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to reset violation level for " + playerUUID, e);
    }
  }

  @Override
  public void resetAllViolationLevels(UUID playerUUID) {
    String sql = "DELETE FROM sloth_punishments WHERE uuid = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, playerUUID.toString());
      ps.executeUpdate();
    } catch (SQLException e) {
      plugin
          .getLogger()
          .log(Level.SEVERE, "Failed to reset all violation levels for " + playerUUID, e);
    }
  }
}
