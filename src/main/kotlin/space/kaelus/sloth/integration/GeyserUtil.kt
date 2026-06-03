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
package space.kaelus.sloth.integration

import java.util.UUID
import org.geysermc.floodgate.api.FloodgateApi

object GeyserUtil {
  private const val FLOODGATE_API_CLASS = "org.geysermc.floodgate.api.FloodgateApi"
  private const val BEDROCK_UUID_PREFIX = "00000000-0000-0000-0009"

  private val floodgatePresent: Boolean = hasClass(FLOODGATE_API_CLASS)

  fun isBedrockPlayer(uuid: UUID): Boolean {
    if (floodgatePresent && FloodgateApi.getInstance().isFloodgatePlayer(uuid)) {
      return true
    }
    return uuid.toString().startsWith(BEDROCK_UUID_PREFIX)
  }

  private fun hasClass(name: String): Boolean = runCatching { Class.forName(name) }.isSuccess
}
