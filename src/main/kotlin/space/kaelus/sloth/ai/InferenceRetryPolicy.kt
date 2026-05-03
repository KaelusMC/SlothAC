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
package space.kaelus.sloth.ai

import space.kaelus.sloth.server.AIServer

object InferenceRetryPolicy {
  fun shouldRetry(throwable: Throwable): Boolean {
    if (throwable !is AIServer.RequestException) return false
    return when (throwable.code) {
      AIServer.ResponseCode.TIMEOUT,
      AIServer.ResponseCode.NETWORK_ERROR,
      AIServer.ResponseCode.SERVER_ERROR,
      AIServer.ResponseCode.SERVICE_UNAVAILABLE,
      AIServer.ResponseCode.RATE_LIMITED -> true
      else -> false
    }
  }
}
