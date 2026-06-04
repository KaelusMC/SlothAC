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
package space.kaelus.sloth.redis

import io.lettuce.core.ClientOptions
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SocketOptions
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import java.time.Duration
import java.util.logging.Level
import java.util.logging.Logger
import space.kaelus.sloth.config.ConfigManager

/**
 * Owns the Lettuce Redis connection used for cross-server features.
 *
 * Mirrors [space.kaelus.sloth.database.DatabaseManager]'s resilient pattern: when `redis.enabled`
 * is off, or the connection cannot be established, the manager logs a warning and stays disabled
 * ([isAvailable] = `false`) instead of failing plugin start-up. Publishing is then a no-op.
 */
class RedisManager(private val configManager: ConfigManager, private val logger: Logger) {
  @Volatile private var client: RedisClient? = null
  @Volatile private var connection: StatefulRedisConnection<String, String>? = null
  @Volatile private var pubSubConnection: StatefulRedisPubSubConnection<String, String>? = null

  @Volatile
  var isAvailable: Boolean = false
    private set

  fun start() {
    if (isAvailable) return
    val config = configManager.config
    if (!config.getBoolean("redis.enabled", false)) return

    val host = config.getString("redis.host", DEFAULT_HOST)
    val port = config.getInt("redis.port", DEFAULT_PORT)
    val database = config.getInt("redis.database", DEFAULT_DATABASE)
    val useSsl = config.getBoolean("redis.ssl", false)
    val timeoutSeconds =
      config.getLong("redis.timeout-seconds", DEFAULT_TIMEOUT_SECONDS).coerceAtLeast(1L)
    val password = config.getString("redis.password", "")

    val uri =
      RedisURI.builder()
        .withHost(host)
        .withPort(port)
        .withDatabase(database)
        .withSsl(useSsl)
        .withTimeout(Duration.ofSeconds(timeoutSeconds))
        .apply { if (password.isNotEmpty()) withPassword(password.toCharArray()) }
        .build()

    runCatching {
        val redisClient = RedisClient.create(uri)
        redisClient.setOptions(
          ClientOptions.builder()
            .socketOptions(
              SocketOptions.builder().connectTimeout(Duration.ofSeconds(timeoutSeconds)).build()
            )
            .build()
        )
        client = redisClient
        connection = redisClient.connect()
        pubSubConnection = redisClient.connectPubSub()
      }
      .onSuccess {
        isAvailable = true
        logger.info("[Redis] Connected to $host:$port (database $database).")
      }
      .onFailure { error ->
        logger.log(
          Level.WARNING,
          "[Redis] Could not connect to $host:$port; cross-server features are disabled.",
          error,
        )
        shutdown()
      }
  }

  /** Publishes [message] to [channel] without blocking the caller. No-op when unavailable. */
  fun publishAsync(channel: String, message: String) {
    val activeConnection = connection
    if (!isAvailable || activeConnection == null) return
    runCatching {
        activeConnection.async().publish(channel, message).exceptionally { error ->
          logger.log(Level.FINE, "[Redis] Publish to $channel failed.", error)
          0L
        }
      }
      .onFailure { error -> logger.log(Level.FINE, "[Redis] Publish to $channel failed.", error) }
  }

  /** Subscribes to [channel] and forwards each received message body to [onMessage]. */
  fun subscribe(channel: String, onMessage: (String) -> Unit) {
    val pubSub = pubSubConnection ?: return
    pubSub.addListener(
      object : RedisPubSubAdapter<String, String>() {
        override fun message(receivedChannel: String, message: String) {
          if (receivedChannel == channel) onMessage(message)
        }
      }
    )
    pubSub.sync().subscribe(channel)
  }

  fun shutdown() {
    isAvailable = false
    runCatching { connection?.close() }
    runCatching { pubSubConnection?.close() }
    runCatching { client?.shutdown(Duration.ZERO, SHUTDOWN_TIMEOUT) }
    connection = null
    pubSubConnection = null
    client = null
  }

  private companion object {
    const val DEFAULT_HOST = "localhost"
    const val DEFAULT_PORT = 6379
    const val DEFAULT_DATABASE = 0
    const val DEFAULT_TIMEOUT_SECONDS = 10L
    val SHUTDOWN_TIMEOUT: Duration = Duration.ofSeconds(2)
  }
}
