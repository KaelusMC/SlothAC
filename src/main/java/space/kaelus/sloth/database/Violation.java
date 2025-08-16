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

import lombok.Data;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
public class Violation {
    private final String serverName;
    private final UUID playerUUID;
    private final String checkName;
    private final String verbose;
    private final int vl;
    private final Date createdAt;

    public static List<Violation> fromResultSet(ResultSet resultSet) throws SQLException {
        List<Violation> violations = new ArrayList<>();
        while(resultSet.next()) {
            String server = resultSet.getString("server");
            UUID player = UUID.fromString(resultSet.getString("uuid"));
            String checkName = resultSet.getString("check_name");
            String verbose = resultSet.getString("verbose");
            int vl = resultSet.getInt("vl");
            Date createdAt = new Date(resultSet.getLong("created_at"));
            violations.add(new Violation(server, player, checkName, verbose, vl, createdAt));
        }
        return violations;
    }
}