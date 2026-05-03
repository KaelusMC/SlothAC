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
import java.nio.ByteOrder
import java.time.Duration
import java.util.concurrent.CompletableFuture
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.ai.AiBatchTransport
import space.kaelus.sloth.ai.AiTransport
import space.kaelus.sloth.ai.snapshot

class AIServer(
  private val plugin: SlothAC,
  url: String,
  private val apiKey: String,
  private val apiCooldown: ApiCooldown,
) : AiTransport, AiBatchTransport {
  private val serverUri: URI = URI.create(url)

  override fun send(payload: ByteBuffer): CompletableFuture<String> {
    if (apiCooldown.isWaiting()) {
      return CompletableFuture.failedFuture(
        RequestException(ResponseCode.WAITING, "Server is in backoff.")
      )
    }

    return sendRequest(payload.snapshot(), batch = false)
  }

  override fun sendBatch(items: List<ByteArray>): CompletableFuture<String> {
    val rejection =
      when {
        apiCooldown.isWaiting() -> RequestException(ResponseCode.WAITING, "Server is in backoff.")
        items.isEmpty() -> RequestException(ResponseCode.BAD_REQUEST, "Empty batch")
        else -> null
      }
    if (rejection != null) return CompletableFuture.failedFuture(rejection)
    return sendRequest(encodeBatchFraming(items), batch = true)
  }

  private fun sendRequest(body: ByteArray, batch: Boolean): CompletableFuture<String> {
    val builder =
      HttpRequest.newBuilder(serverUri)
        .header("Content-Type", "application/octet-stream")
        .header("User-Agent", "SlothAC/" + plugin.description.version)
        .header("X-API-Key", apiKey)
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
        .timeout(if (batch) BATCH_REQUEST_TIMEOUT else REQUEST_TIMEOUT)
    if (batch) {
      builder.header("X-Sloth-Batch", "1")
    }

    return HTTP_CLIENT.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
      .thenApply { response -> catchResponse(response) }
      .exceptionallyCompose { throwable -> catchException(throwable) }
  }

  private fun encodeBatchFraming(items: List<ByteArray>): ByteArray {
    check(items.size <= BATCH_MAX_ITEMS) {
      "Batch count ${items.size} exceeds wire-format max $BATCH_MAX_ITEMS"
    }
    val totalSize = BATCH_COUNT_SIZE + items.sumOf { BATCH_ITEM_HEADER_SIZE + it.size }
    val buf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
    buf.putShort(items.size.toShort())
    for (item in items) {
      buf.putInt(item.size)
      buf.put(item)
    }
    return buf.array()
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
    NOT_FOUND(404),
    PAYLOAD_TOO_LARGE(413),
    INVALID_SEQUENCE(422),
    RATE_LIMITED(429),
    SERVER_ERROR(500),
    SERVICE_UNAVAILABLE(503),
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
    private val BATCH_REQUEST_TIMEOUT = Duration.ofSeconds(10)

    private const val BATCH_COUNT_SIZE = 2
    private const val BATCH_ITEM_HEADER_SIZE = 4
    const val BATCH_MAX_ITEMS = 256

    private val HTTP_CLIENT: HttpClient =
      HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(CONNECT_TIMEOUT)
        .build()

    fun shutdownHttpClient() {
      runCatching { (HTTP_CLIENT as? AutoCloseable)?.close() }
    }
  }
}
