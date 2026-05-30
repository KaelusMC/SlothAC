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
package space.kaelus.sloth.connect

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.config.ConfigManager

sealed interface StartResult {
  data class Started(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val verificationUriComplete: String,
    val expiresInSeconds: Long,
    val intervalSeconds: Long,
  ) : StartResult

  data class Error(val message: String) : StartResult
}

sealed interface PollResult {
  data object Pending : PollResult

  data class SlowDown(val intervalSeconds: Long) : PollResult

  data class Approved(
    val secretKey: String,
    val serverId: String?,
    val serverName: String?,
    val needsPlan: Boolean,
    val allowlistedIp: String?,
  ) : PollResult

  data object Denied : PollResult

  data object Expired : PollResult

  data class Error(val message: String) : PollResult
}

sealed interface RevokeResult {
  data object Revoked : RevokeResult

  data class Error(val message: String) : RevokeResult
}

@Suppress("TooGenericExceptionCaught", "ReturnCount")
class ConnectService(private val plugin: SlothAC, private val configManager: ConfigManager) {
  private val mapper = ObjectMapper()
  private val client: HttpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build()

  fun start(): StartResult {
    return try {
      val (code, node) =
        post(
          "/api/v1/device/start",
          mapOf("client_id" to CLIENT_ID, "plugin_version" to plugin.description.version),
        ) ?: return StartResult.Error("Panel URL is not configured.")
      when (code) {
        HTTP_OK -> {
          val deviceCode = node.path("device_code").asText("")
          val userCode = node.path("user_code").asText("")
          if (deviceCode.isBlank() || userCode.isBlank()) {
            StartResult.Error("Panel returned an invalid response.")
          } else {
            StartResult.Started(
              deviceCode = deviceCode,
              userCode = userCode,
              verificationUri = node.path("verification_uri").asText(""),
              verificationUriComplete = node.path("verification_uri_complete").asText(""),
              expiresInSeconds =
                node
                  .path("expires_in")
                  .asLong(DEFAULT_EXPIRES)
                  .coerceIn(MIN_EXPIRES_SECONDS, MAX_EXPIRES_SECONDS),
              intervalSeconds =
                node
                  .path("interval")
                  .asLong(DEFAULT_INTERVAL)
                  .coerceIn(MIN_INTERVAL_SECONDS, MAX_INTERVAL_SECONDS),
            )
          }
        }
        TOO_MANY_REQUESTS -> StartResult.Error("Too many attempts. Please wait a few minutes.")
        else -> StartResult.Error("Panel returned HTTP $code.")
      }
    } catch (e: Exception) {
      StartResult.Error("Could not reach the panel: ${e.message}")
    }
  }

  fun poll(deviceCode: String): PollResult {
    return try {
      val (code, node) =
        post("/api/v1/device/token", mapOf("device_code" to deviceCode))
          ?: return PollResult.Error("Panel URL is not configured.")
      if (code == HTTP_OK && node.path("status").asText("") == "approved") {
        val secret = node.path("secret_key").asText("")
        if (secret.isBlank()) {
          return PollResult.Error("Panel approved but returned no key.")
        }
        val server = node.path("server")
        PollResult.Approved(
          secretKey = secret,
          serverId = server.path("id").asText("").ifBlank { null },
          serverName = server.path("name").asText("").ifBlank { null },
          needsPlan = server.path("needs_plan").asBoolean(false),
          allowlistedIp = node.path("allowlisted_ip").asText("").ifBlank { null },
        )
      } else {
        when (node.path("error").asText("")) {
          "authorization_pending" -> PollResult.Pending
          "slow_down" ->
            PollResult.SlowDown(
              node
                .path("interval")
                .asLong(DEFAULT_INTERVAL + SLOW_DOWN_EXTRA_SECONDS)
                .coerceIn(MIN_INTERVAL_SECONDS, MAX_INTERVAL_SECONDS)
            )
          "access_denied" -> PollResult.Denied
          "expired_token" -> PollResult.Expired
          else -> PollResult.Error("Unexpected panel response (HTTP $code).")
        }
      }
    } catch (e: Exception) {
      PollResult.Error("Network error: ${e.message}")
    }
  }

  fun revoke(secretKey: String): RevokeResult {
    return try {
      val (code, node) =
        post("/api/v1/device/revoke", mapOf("secret_key" to secretKey))
          ?: return RevokeResult.Error("Panel URL is not configured.")
      val status = node.path("status").asText("")
      when {
        code == HTTP_OK && (status == "revoked" || status == "not_found") -> RevokeResult.Revoked
        code == TOO_MANY_REQUESTS ->
          RevokeResult.Error("Too many attempts. Please wait a few minutes.")
        else -> RevokeResult.Error("Panel returned HTTP $code.")
      }
    } catch (e: Exception) {
      RevokeResult.Error("Network error: ${e.message}")
    }
  }

  fun cancel(deviceCode: String) {
    try {
      post("/api/v1/device/cancel", mapOf("device_code" to deviceCode))
    } catch (e: Exception) {
      plugin.logger.fine("[Connect] cancel failed: ${e.message}")
    }
  }

  private fun post(path: String, body: Map<String, Any?>): Pair<Int, JsonNode>? {
    val base = configManager.connectPanelUrl.trim().trimEnd('/')
    if (base.isBlank()) return null
    val request =
      HttpRequest.newBuilder(URI.create("$base$path"))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("User-Agent", "SlothAC/" + plugin.description.version)
        .timeout(REQUEST_TIMEOUT)
        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
        .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    val payload = response.body()
    val node =
      if (payload.isNullOrBlank()) mapper.createObjectNode()
      else runCatching { mapper.readTree(payload) }.getOrElse { mapper.createObjectNode() }
    return response.statusCode() to node
  }

  private companion object {
    const val CLIENT_ID = "slothac-plugin"
    const val HTTP_OK = 200
    const val TOO_MANY_REQUESTS = 429
    const val DEFAULT_EXPIRES = 600L
    const val DEFAULT_INTERVAL = 5L
    const val MIN_EXPIRES_SECONDS = 60L
    const val MAX_EXPIRES_SECONDS = 3600L
    const val MIN_INTERVAL_SECONDS = 1L
    const val MAX_INTERVAL_SECONDS = 300L
    const val SLOW_DOWN_EXTRA_SECONDS = 5L
    val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(10)
    val REQUEST_TIMEOUT: Duration = Duration.ofSeconds(15)
  }
}
