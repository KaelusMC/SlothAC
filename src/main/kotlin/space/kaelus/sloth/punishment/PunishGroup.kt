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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package space.kaelus.sloth.punishment

import java.util.Locale
import java.util.NavigableMap
import space.kaelus.sloth.checks.ICheck

class PunishGroup(
  val groupName: String,
  checkNames: List<String>,
  val actions: NavigableMap<Int, List<String>>,
) {
  val associatedCheckNames: Set<String> = checkNames.map { it.lowercase(Locale.ROOT) }.toSet()

  fun isCheckAssociated(check: ICheck): Boolean {
    val checkNameLower = check.checkName.lowercase(Locale.ROOT)
    for (filter in associatedCheckNames) {
      if (checkNameLower.contains(filter)) {
        return true
      }
    }
    return false
  }
}
