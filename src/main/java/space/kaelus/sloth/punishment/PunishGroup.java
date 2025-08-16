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
package space.kaelus.sloth.punishment;

import lombok.Getter;
import space.kaelus.sloth.checks.AbstractCheck;

import java.util.*;

@Getter
public class PunishGroup {
    private final String groupName;
    private final Set<String> associatedCheckNames;
    private final NavigableMap<Integer, List<String>> actions;

    public PunishGroup(String groupName, List<String> checkNames, NavigableMap<Integer, List<String>> actions) {
        this.groupName = groupName;
        this.associatedCheckNames = new HashSet<>(checkNames.stream().map(String::toLowerCase).toList());
        this.actions = actions;
    }

    public boolean isCheckAssociated(AbstractCheck check) {
        String checkNameLower = check.getCheckName().toLowerCase(Locale.ROOT);
        for (String filter : associatedCheckNames) {
            if (checkNameLower.contains(filter)) {
                return true;
            }
        }
        return false;
    }
}