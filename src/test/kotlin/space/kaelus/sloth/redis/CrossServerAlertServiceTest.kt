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
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import java.util.logging.Logger
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import space.kaelus.sloth.alert.AlertManager
import space.kaelus.sloth.alert.AlertType
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.config.ConfigView
import space.kaelus.sloth.config.LocaleManager
import space.kaelus.sloth.utils.MessageUtil

class CrossServerAlertServiceTest {
  private val gson = GsonComponentSerializer.gson()
  private val mapper = ObjectMapper()

  @BeforeEach
  fun setUp() {
    // Initialise the real MessageUtil so the receive path can render the [server] prefix.
    val localeManager = mockk<LocaleManager>(relaxed = true)
    every { localeManager.getRawMessage(any()) } returns "[<server>]"
    MessageUtil.init(
      localeManager,
      mockk<BukkitAudiences>(relaxed = true),
      Logger.getLogger("test"),
    )
  }

  @Test
  fun `publishes only the enabled alert types`() {
    val redis = mockk<RedisManager>(relaxed = true)
    every { redis.isAvailable } returns true
    val alerts = mockk<AlertManager>(relaxed = true)
    val service =
      service(
        """
        cross-server:
          enabled: true
          server-name: "Lobby"
          channel: "test:alerts"
          alerts:
            regular: true
            suspicious: false
        """
          .trimIndent(),
        redis,
        alerts,
      )

    service.start()
    service.publish(AlertType.REGULAR, Component.text("a"))
    service.publish(AlertType.SUSPICIOUS, Component.text("b"))
    service.publish(AlertType.BRAND, Component.text("c"))

    verify(exactly = 1) { redis.publishAsync("test:alerts", any()) }
    verify(exactly = 1) { alerts.crossServerPublisher = any() }
    verify(exactly = 1) { redis.subscribe("test:alerts", any()) }
  }

  @Test
  fun `does nothing when cross-server is disabled`() {
    val redis = mockk<RedisManager>(relaxed = true)
    val alerts = mockk<AlertManager>(relaxed = true)
    val service = service("cross-server:\n  enabled: false\n", redis, alerts)

    service.start()
    service.publish(AlertType.REGULAR, Component.text("a"))

    verify(exactly = 0) { redis.start() }
    verify(exactly = 0) { redis.subscribe(any(), any()) }
    verify(exactly = 0) { redis.publishAsync(any(), any()) }
    verify(exactly = 0) { alerts.crossServerPublisher = any() }
  }

  @Test
  fun `stays local when redis is unavailable`() {
    val redis = mockk<RedisManager>(relaxed = true)
    every { redis.isAvailable } returns false
    val alerts = mockk<AlertManager>(relaxed = true)
    val service = service("cross-server:\n  enabled: true\n", redis, alerts)

    service.start()
    service.publish(AlertType.REGULAR, Component.text("a"))

    verify(exactly = 1) { redis.start() }
    verify(exactly = 0) { redis.subscribe(any(), any()) }
    verify(exactly = 0) { redis.publishAsync(any(), any()) }
    verify(exactly = 0) { alerts.crossServerPublisher = any() }
  }

  @Test
  fun `delivers foreign alerts and ignores its own`() {
    val redis = mockk<RedisManager>(relaxed = true)
    every { redis.isAvailable } returns true
    val incoming = slot<(String) -> Unit>()
    every { redis.subscribe(any(), capture(incoming)) } just runs
    val published = slot<String>()
    every { redis.publishAsync(any(), capture(published)) } just runs
    val alerts = mockk<AlertManager>(relaxed = true)
    val service =
      service(
        """
        cross-server:
          enabled: true
          server-name: "Lobby"
          channel: "test:alerts"
          alerts:
            regular: true
            suspicious: true
        """
          .trimIndent(),
        redis,
        alerts,
      )
    service.start()

    // A message this server published must be ignored when it loops back.
    service.publish(AlertType.REGULAR, Component.text("local"))
    incoming.captured.invoke(published.captured)
    verify(exactly = 0) { alerts.deliver(any(), any()) }

    // A message from another server is delivered locally.
    val foreign =
      mapper.writeValueAsString(
        CrossServerAlert("other-origin", "PvP", "REGULAR", gson.serialize(Component.text("remote")))
      )
    incoming.captured.invoke(foreign)
    verify(exactly = 1) { alerts.deliver(any(), AlertType.REGULAR) }
  }

  @Test
  fun `ignores malformed and unknown-type messages`() {
    val redis = mockk<RedisManager>(relaxed = true)
    every { redis.isAvailable } returns true
    val incoming = slot<(String) -> Unit>()
    every { redis.subscribe(any(), capture(incoming)) } just runs
    val alerts = mockk<AlertManager>(relaxed = true)
    val service = service("cross-server:\n  enabled: true\n", redis, alerts)
    service.start()

    incoming.captured.invoke("not even json")
    incoming.captured.invoke(
      mapper.writeValueAsString(
        CrossServerAlert("o", "PvP", "NONSENSE", gson.serialize(Component.text("x")))
      )
    )

    verify(exactly = 0) { alerts.deliver(any(), any()) }
  }

  private fun service(
    yaml: String,
    redis: RedisManager,
    alerts: AlertManager,
  ): CrossServerAlertService {
    val configManager = mockk<ConfigManager>()
    every { configManager.config } returns configView(yaml)
    return CrossServerAlertService(
      configManager,
      redis,
      alerts,
      Logger.getLogger("cross-server-test"),
    )
  }

  private fun configView(yaml: String): ConfigView {
    val loader = YamlConfigurationLoader.builder().source { yaml.reader().buffered() }.build()
    return ConfigView(loader.load())
  }
}
