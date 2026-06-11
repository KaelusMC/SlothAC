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
import space.kaelus.sloth.scheduler.SchedulerService
import space.kaelus.sloth.utils.Message
import space.kaelus.sloth.utils.MessageUtil

class CrossServerAlertService(
  private val configManager: ConfigManager,
  private val redisManager: RedisManager,
  private val alertManager: AlertManager,
  private val scheduler: SchedulerService,
  private val logger: Logger,
) {
  private val origin: String = UUID.randomUUID().toString()
  private val mapper = ObjectMapper()
  private val componentSerializer = GsonComponentSerializer.gson()

  @Volatile private var enabled = false
  @Volatile private var mirroredTypes: Set<AlertType> = emptySet()
  @Volatile private var serverName = DEFAULT_SERVER_NAME
  @Volatile private var channel = DEFAULT_CHANNEL

  fun start() {
    val config = configManager.config
    if (!config.getBoolean("cross-server.enabled", false)) return

    serverName = config.getString("cross-server.server-name", DEFAULT_SERVER_NAME)
    channel = config.getString("cross-server.channel", DEFAULT_CHANNEL)
    val types = EnumSet.noneOf(AlertType::class.java)
    if (config.getBoolean("cross-server.alerts.regular", true)) types.add(AlertType.REGULAR)
    if (config.getBoolean("cross-server.alerts.suspicious", true)) types.add(AlertType.SUSPICIOUS)
    mirroredTypes = types

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
    if (!enabled) return
    val alert = mapper.readValue(raw, CrossServerAlert::class.java)
    if (alert.origin == origin) return
    val type = runCatching { AlertType.valueOf(alert.type) }.getOrNull() ?: return
    if (type !in mirroredTypes) return
    val body = stripClickEvents(componentSerializer.deserialize(alert.component))
    val prefix = MessageUtil.getMessage(Message.CROSS_SERVER_ALERT_PREFIX, "server", alert.server)
    val message = prefix.append(Component.space()).append(body)
    scheduler.runSync { alertManager.deliver(message, type) }
  }

  private fun stripClickEvents(component: Component): Component {
    val stripped = component.clickEvent(null)
    val children = component.children()
    if (children.isEmpty()) {
      return stripped
    }
    return stripped.children(children.map(::stripClickEvents))
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
