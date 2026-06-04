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

import com.fasterxml.jackson.databind.ObjectMapper
import java.util.EnumSet
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import space.kaelus.sloth.alert.AlertManager
import space.kaelus.sloth.alert.AlertType
import space.kaelus.sloth.alert.CrossServerPublisher
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.utils.Message
import space.kaelus.sloth.utils.MessageUtil

/**
 * Bridges the local [AlertManager] with other servers over Redis pub/sub.
 *
 * When enabled, it publishes locally-raised [AlertType.REGULAR] and [AlertType.SUSPICIOUS] alerts
 * to the shared channel and, in the other direction, delivers alerts received from other servers to
 * local staff with a `[server]` origin tag. Alerts received over Redis are delivered through
 * [AlertManager.deliver] so they are never mirrored back out, and messages this server published
 * are dropped on arrival via the per-process [origin] id.
 */
class CrossServerAlertService(
  private val configManager: ConfigManager,
  private val redisManager: RedisManager,
  private val alertManager: AlertManager,
  private val logger: Logger,
) {
  private val origin: String = UUID.randomUUID().toString()
  private val mapper = ObjectMapper()
  private val componentSerializer = GsonComponentSerializer.gson()
  private val mirroredTypes: MutableSet<AlertType> = EnumSet.noneOf(AlertType::class.java)

  @Volatile private var enabled = false
  private var serverName = DEFAULT_SERVER_NAME
  private var channel = DEFAULT_CHANNEL

  fun start() {
    val config = configManager.config
    if (!config.getBoolean("cross-server.enabled", false)) return

    serverName = config.getString("cross-server.server-name", DEFAULT_SERVER_NAME)
    channel = config.getString("cross-server.channel", DEFAULT_CHANNEL)
    mirroredTypes.clear()
    if (config.getBoolean("cross-server.alerts.regular", true)) mirroredTypes.add(AlertType.REGULAR)
    if (config.getBoolean("cross-server.alerts.suspicious", true)) {
      mirroredTypes.add(AlertType.SUSPICIOUS)
    }

    redisManager.start()
    if (!redisManager.isAvailable) {
      logger.warning(
        "[CrossServer] cross-server.enabled is true but Redis is unavailable; alerts stay local."
      )
      return
    }

    redisManager.subscribe(channel, ::onMessage)
    alertManager.crossServerPublisher = CrossServerPublisher { type, component ->
      publish(type, component)
    }
    enabled = true
    logger.info(
      "[CrossServer] Mirroring ${mirroredTypes.joinToString(", ")} alerts as " +
        "\"$serverName\" on channel \"$channel\"."
    )
  }

  /**
   * Mirrors a locally-raised alert to other servers. Wired into [AlertManager] as the publisher.
   */
  fun publish(type: AlertType, component: Component) {
    if (!enabled || type !in mirroredTypes) return
    val payload =
      runCatching {
          val alert =
            CrossServerAlert(
              origin,
              serverName,
              type.name,
              componentSerializer.serialize(component),
            )
          mapper.writeValueAsString(alert)
        }
        .getOrElse { error ->
          logger.log(Level.FINE, "[CrossServer] Failed to serialize alert.", error)
          return
        }
    redisManager.publishAsync(channel, payload)
  }

  private fun onMessage(raw: String) {
    runCatching { handleMessage(raw) }
      .onFailure { error ->
        logger.log(Level.FINE, "[CrossServer] Failed to handle incoming alert.", error)
      }
  }

  @Suppress("ReturnCount")
  private fun handleMessage(raw: String) {
    val alert = mapper.readValue(raw, CrossServerAlert::class.java)
    if (alert.origin == origin) return // our own alert, already shown locally
    val type = runCatching { AlertType.valueOf(alert.type) }.getOrNull() ?: return
    if (type !in mirroredTypes) return
    val body = componentSerializer.deserialize(alert.component)
    val prefix = MessageUtil.getMessage(Message.CROSS_SERVER_ALERT_PREFIX, "server", alert.server)
    alertManager.deliver(prefix.append(Component.space()).append(body), type)
  }

  fun shutdown() {
    enabled = false
    alertManager.crossServerPublisher = null
  }

  private companion object {
    const val DEFAULT_SERVER_NAME = "server-1"
    const val DEFAULT_CHANNEL = "slothac:alerts"
  }
}
