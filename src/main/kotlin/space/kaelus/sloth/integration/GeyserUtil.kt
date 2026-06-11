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
  private const val GEYSER_API_CLASS = "org.geysermc.geyser.api.GeyserApi"
  private const val BEDROCK_UUID_PREFIX = "00000000-0000-0000-0009"

  private val floodgatePresent: Boolean = hasClass(FLOODGATE_API_CLASS)
  private val geyserPresent: Boolean = hasClass(GEYSER_API_CLASS)

  @Volatile private var geyserApiInstance: Any? = null

  private val geyserIsBedrockMethod by lazy {
    runCatching { Class.forName(GEYSER_API_CLASS).getMethod("isBedrockPlayer", UUID::class.java) }
      .getOrNull()
  }

  private fun geyserApi(): Any? {
    if (!geyserPresent) {
      return null
    }
    var api = geyserApiInstance
    if (api == null) {
      api =
        runCatching { Class.forName(GEYSER_API_CLASS).getMethod("api").invoke(null) }.getOrNull()
      if (api != null) {
        geyserApiInstance = api
      }
    }
    return api
  }

  fun isBedrockPlayer(uuid: UUID): Boolean =
    isFloodgateBedrock(uuid) ||
      isGeyserBedrock(uuid) ||
      uuid.toString().startsWith(BEDROCK_UUID_PREFIX)

  private fun isFloodgateBedrock(uuid: UUID): Boolean =
    floodgatePresent && FloodgateApi.getInstance().isFloodgatePlayer(uuid)

  private fun isGeyserBedrock(uuid: UUID): Boolean {
    val api = geyserApi()
    val method = geyserIsBedrockMethod
    if (api == null || method == null) {
      return false
    }
    return runCatching { method.invoke(api, uuid) as Boolean }.getOrDefault(false)
  }

  private fun hasClass(name: String): Boolean = runCatching { Class.forName(name) }.isSuccess
}
