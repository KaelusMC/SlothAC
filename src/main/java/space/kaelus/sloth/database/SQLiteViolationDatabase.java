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

import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.player.SlothPlayer;
import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class SQLiteViolationDatabase implements ViolationDatabase {
    private Connection connection;

    @Override
    public synchronized void connect() {
        File dbFile = new File(SlothAC.getInstance().getDataFolder(), "violations.db");
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS violations(" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "server VARCHAR(255) NOT NULL, " +
                        "uuid CHAR(36) NOT NULL, " +
                        "check_name TEXT NOT NULL, " +
                        "verbose TEXT NOT NULL, " +
                        "vl INTEGER NOT NULL, " +
                        "created_at BIGINT NOT NULL);");

                statement.execute("CREATE INDEX IF NOT EXISTS idx_violations_uuid ON violations(uuid);");

                statement.execute("CREATE TABLE IF NOT EXISTS sloth_punishments (" +
                        "uuid CHAR(36) NOT NULL, " +
                        "punish_group VARCHAR(255) NOT NULL, " +
                        "vl INTEGER NOT NULL, " +
                        "PRIMARY KEY (uuid, punish_group));");
            }
        } catch (SQLException | ClassNotFoundException e) {
            SlothAC.getInstance().getLogger().log(Level.SEVERE, "Failed to connect to SQLite database", e);
        }
    }

    @Override
    public synchronized void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            SlothAC.getInstance().getLogger().log(Level.SEVERE, "Failed to close SQLite connection", e);
        }
    }

    @Override
    public synchronized void logAlert(SlothPlayer player, String verbose, String checkName, int vls) {
        String sql = "INSERT INTO violations (server, uuid, check_name, verbose, vl, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, SlothAC.getInstance().getConfigManager().getConfig().getString("history.server-name", "server"));
            ps.setString(2, player.getUuid().toString());
            ps.setString(3, checkName);
            ps.setString(4, verbose);
            ps.setInt(5, vls);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            SlothAC.getInstance().getLogger().log(Level.SEVERE, "Failed to log violation", e);
        }
    }

    @Override
    public synchronized int getLogCount(UUID player) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM violations WHERE uuid = ?")) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            SlothAC.getInstance().getLogger().log(Level.SEVERE, "Failed to count violations", e);
        }
        return 0;
    }

    @Override
    public synchronized List<Violation> getViolations(UUID player, int page, int limit) {
        String sql = "SELECT * FROM violations WHERE uuid = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, player.toString());
            ps.setInt(2, limit);
            ps.setInt(3, (page - 1) * limit);
            try (ResultSet rs = ps.executeQuery()) {
                return Violation.fromResultSet(rs);
            }
        } catch (SQLException e) {
            SlothAC.getInstance().getLogger().log(Level.SEVERE, "Failed to get violations", e);
        }
        return List.of();
    }

    @Override
    public synchronized int getViolationLevel(UUID playerUUID, String punishGroupName) {
        String sql = "SELECT vl FROM sloth_punishments WHERE uuid = ? AND punish_group = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            ps.setString(2, punishGroupName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("vl");
                }
            }
        } catch (SQLException e) {
            SlothAC.getInstance().getLogger().log(Level.SEVERE, "Failed to get violation level for " + playerUUID, e);
        }
        return 0;
    }

    @Override
    public synchronized int incrementViolationLevel(UUID playerUUID, String punishGroupName) {
        int currentVl = getViolationLevel(playerUUID, punishGroupName);
        int newVl = currentVl + 1;

        String sql = "INSERT INTO sloth_punishments (uuid, punish_group, vl) VALUES (?, ?, ?) " +
                "ON CONFLICT(uuid, punish_group) DO UPDATE SET vl = excluded.vl";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            ps.setString(2, punishGroupName);
            ps.setInt(3, newVl);
            ps.executeUpdate();
            return newVl;
        } catch (SQLException e) {
            SlothAC.getInstance().getLogger().log(Level.SEVERE, "Failed to increment violation level for " + playerUUID, e);
        }
        return currentVl;
    }

    @Override
    public synchronized void resetViolationLevel(UUID playerUUID, String punishGroupName) {
        String sql = "DELETE FROM sloth_punishments WHERE uuid = ? AND punish_group = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            ps.setString(2, punishGroupName);
            ps.executeUpdate();
        } catch (SQLException e) {
            SlothAC.getInstance().getLogger().log(Level.SEVERE, "Failed to reset violation level for " + playerUUID, e);
        }
    }

    @Override
    public synchronized void resetAllViolationLevels(UUID playerUUID) {
        String sql = "DELETE FROM sloth_punishments WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            SlothAC.getInstance().getLogger().log(Level.SEVERE, "Failed to reset all violation levels for " + playerUUID, e);
        }
    }
}