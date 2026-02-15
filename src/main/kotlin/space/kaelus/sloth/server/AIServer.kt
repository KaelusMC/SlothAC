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
package space.kaelus.sloth.server

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.CompletableFuture
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.ai.AiTransport

class AIServer(
  private val plugin: SlothAC,
  url: String,
  private val apiKey: String,
  private val apiCooldown: ApiCooldown,
) : AiTransport {
  private val serverUri: URI = URI.create(url)

  override fun send(payload: ByteBuffer): CompletableFuture<String> {
    if (apiCooldown.isWaiting()) {
      return CompletableFuture.failedFuture(
        RequestException(ResponseCode.WAITING, "Server is in backoff.")
      )
    }

    if (!payload.hasArray()) {
      val data = ByteArray(payload.remaining())
      payload.get(data)
      return sendBytes(data)
    }

    val request =
      HttpRequest.newBuilder(serverUri)
        .header("Content-Type", "application/octet-stream")
        .header("User-Agent", "SlothAC/" + plugin.pluginMeta.version)
        .header("X-API-Key", apiKey)
        .header("Accept", "application/json")
        .POST(
          HttpRequest.BodyPublishers.ofByteArray(
            payload.array(),
            payload.arrayOffset() + payload.position(),
            payload.remaining(),
          )
        )
        .timeout(REQUEST_TIMEOUT)
        .build()

    return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
      .thenApply { response -> catchResponse(response) }
      .exceptionallyCompose { throwable -> catchException(throwable) }
  }

  private fun sendBytes(playerData: ByteArray): CompletableFuture<String> {
    if (apiCooldown.isWaiting()) {
      return CompletableFuture.failedFuture(
        RequestException(ResponseCode.WAITING, "Server is in backoff.")
      )
    }
    val request =
      HttpRequest.newBuilder(serverUri)
        .header("Content-Type", "application/octet-stream")
        .header("User-Agent", "SlothAC/" + plugin.pluginMeta.version)
        .header("X-API-Key", apiKey)
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofByteArray(playerData))
        .timeout(REQUEST_TIMEOUT)
        .build()

    return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
      .thenApply { response -> catchResponse(response) }
      .exceptionallyCompose { throwable -> catchException(throwable) }
  }

  private fun catchResponse(response: HttpResponse<String>): String {
    val statusCode = response.statusCode()
    if (statusCode >= 300 || statusCode < 200) {
      if (statusCode >= 500 || statusCode == 403) {
        apiCooldown.recordFailure()
      }

      throw RequestException(
        ResponseCode.fromStatusCode(statusCode),
        "HTTP Status $statusCode: ${response.body()}",
        responseBody = response.body(),
      )
    }

    apiCooldown.recordSuccess()
    return response.body()
  }

  private fun <U> catchException(throwable: Throwable): CompletableFuture<U> {
    val cause =
      if (throwable is java.util.concurrent.CompletionException && throwable.cause != null) {
        throwable.cause!!
      } else {
        throwable
      }
    if (cause is RequestException) {
      return CompletableFuture.failedFuture(cause)
    }

    if (cause !is HttpTimeoutException) {
      apiCooldown.recordFailure()
    }

    val code =
      if (cause is HttpTimeoutException) ResponseCode.TIMEOUT else ResponseCode.NETWORK_ERROR

    return CompletableFuture.failedFuture(
      RequestException(code, "Request failed: " + cause.message, cause)
    )
  }

  enum class ResponseCode(val httpCode: Int) {
    SUCCESS(200),
    BAD_REQUEST(400),
    UNAUTHORIZED(403),
    INVALID_SEQUENCE(422),
    SERVER_ERROR(500),
    TIMEOUT(-1),
    NETWORK_ERROR(-2),
    PARSE_ERROR(-3),
    WAITING(-5),
    UNKNOWN_ERROR(-4);

    companion object {
      @JvmStatic
      fun fromStatusCode(code: Int): ResponseCode {
        for (value in entries) if (value.httpCode == code) return value
        return if (code >= 500) SERVER_ERROR else if (code >= 400) BAD_REQUEST else UNKNOWN_ERROR
      }
    }
  }

  class RequestException : RuntimeException {
    val code: ResponseCode
    val responseBody: String?

    constructor(
      code: ResponseCode,
      message: String,
      responseBody: String? = null,
    ) : super(message) {
      this.code = code
      this.responseBody = responseBody
    }

    constructor(code: ResponseCode, message: String, cause: Throwable) : super(message, cause) {
      this.code = code
      this.responseBody = null
    }
  }

  companion object {
    private val CONNECT_TIMEOUT = Duration.ofSeconds(10)
    private val REQUEST_TIMEOUT = Duration.ofSeconds(5)

    private val HTTP_CLIENT: HttpClient =
      HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(CONNECT_TIMEOUT)
        .build()
  }
}
