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

import com.google.gson.JsonParser
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import space.kaelus.sloth.data.TickData
import space.kaelus.sloth.server.AIServer
import space.kaelus.sloth.server.AIServerProvider

class DefaultAiService(
  private val transportProvider: AIServerProvider,
  private val serializer: AiSerializer,
  private val parser: AiResponseParser,
) : AiService {
  override val isEnabled: Boolean
    get() = transportProvider.get() != null

  override fun request(ticks: Array<TickData>, count: Int): CompletableFuture<AiResult> {
    val transport: AiTransport =
      transportProvider.get() ?: return CompletableFuture.completedFuture(AiResult.disabledResult())

    val payload: ByteBuffer = serializer.serialize(ticks, count)
    return transport.send(payload).thenApply(this::parse).exceptionallyCompose(this::handleError)
  }

  private fun parse(raw: String): AiResult {
    return try {
      AiResult(parser.parse(raw), raw, null, false)
    } catch (e: Exception) {
      AiResult(null, raw, e, false)
    }
  }

  private fun handleError(error: Throwable): CompletableFuture<AiResult> {
    val cause =
      if (error is java.util.concurrent.CompletionException && error.cause != null) {
        error.cause!!
      } else {
        error
      }

    if (
      cause is AIServer.RequestException && cause.code == AIServer.ResponseCode.INVALID_SEQUENCE
    ) {
      val sequence = parseSequence(cause.responseBody)
      if (sequence != null) {
        return CompletableFuture.failedFuture(AiServiceException(cause, sequence))
      }
    }

    return CompletableFuture.failedFuture(cause)
  }

  internal fun parseSequence(body: String?): Int? {
    if (body.isNullOrBlank()) return null
    return try {
      JsonParser.parseString(body).asJsonObject.getAsJsonObject("details")?.get("sequence")?.asInt
    } catch (_: Exception) {
      null
    }
  }
}
