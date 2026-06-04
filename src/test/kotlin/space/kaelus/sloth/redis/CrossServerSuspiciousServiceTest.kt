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
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.bukkit.entity.Player
import org.junit.jupiter.api.Test
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import space.kaelus.sloth.checks.CheckManager
import space.kaelus.sloth.checks.impl.ai.AiCheck
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.config.ConfigView
import space.kaelus.sloth.player.PlayerDataManager
import space.kaelus.sloth.player.SlothPlayer
import space.kaelus.sloth.scheduler.SchedulerService

class CrossServerSuspiciousServiceTest {
  private val mapper = ObjectMapper()

  private val enabledYaml =
    """
    cross-server:
      enabled: true
      server-name: "Lobby"
      channel: "test:alerts"
      alerts:
        suspicious: true
      suspicious-sync:
        ttl-seconds: 30
        refresh-seconds: 10
    """
      .trimIndent()

  @Test
  fun `payload round-trips through jackson`() {
    val original = SuspiciousSnapshot("Lobby", UUID.randomUUID().toString(), "Notch", 27.5, 42)

    val restored =
      mapper.readValue(mapper.writeValueAsString(original), SuspiciousSnapshot::class.java)

    assertEquals(original, restored)
  }

  @Test
  fun `publishes only suspicious players to redis when enabled`() {
    val redis = mockk<RedisManager>(relaxed = true)
    every { redis.isAvailable } returns true
    val scheduler = mockk<SchedulerService>(relaxed = true)
    val refresh = slot<Runnable>()
    every { scheduler.runTimer(capture(refresh), any(), any()) } returns mockk(relaxed = true)
    val suspect = UUID.randomUUID()
    val playerData = mockk<PlayerDataManager>()
    every { playerData.getPlayers() } returns
      listOf(player(suspect, "Cheater", 30.0, 50), player(UUID.randomUUID(), "Calm", 0.0, 40))
    val keys = mutableListOf<String>()
    every { redis.setWithTtl(capture(keys), any(), any()) } just runs

    val service = service(enabledYaml, redis, scheduler, playerData)
    service.start()
    refresh.captured.run() // one refresh cycle

    assertTrue(service.isActive)
    assertEquals(1, keys.size, "only the player with buffer > 0 should be published")
    assertTrue(keys.single().endsWith(":Lobby:$suspect"))
  }

  @Test
  fun `does not touch redis when suspicious-sync is disabled`() {
    val redis = mockk<RedisManager>(relaxed = true)
    val scheduler = mockk<SchedulerService>(relaxed = true)
    val service =
      service(
        "cross-server:\n  enabled: true\n  alerts:\n    suspicious: false\n",
        redis,
        scheduler,
        mockk(relaxed = true),
      )

    service.start()

    assertFalse(service.isActive)
    verify(exactly = 0) { redis.start() }
    verify(exactly = 0) { scheduler.runTimer(any(), any(), any()) }
    verify(exactly = 0) { redis.setWithTtl(any(), any(), any()) }
  }

  @Test
  fun `fetchRemote returns other servers and ignores own and malformed entries`() {
    val redis = mockk<RedisManager>(relaxed = true)
    every { redis.isAvailable } returns true
    every { redis.scanValues(any()) } returns
      listOf(
        mapper.writeValueAsString(SuspiciousSnapshot("Lobby", "u1", "Self", 10.0, 5)),
        mapper.writeValueAsString(SuspiciousSnapshot("PvP", "u2", "Remote", 22.0, 7)),
        "not json",
      )
    val service = service(enabledYaml, redis, mockk(relaxed = true), mockk(relaxed = true))
    service.start()

    val remote = service.fetchRemote()

    assertEquals(1, remote.size)
    assertEquals("PvP", remote.single().server)
    assertEquals("Remote", remote.single().name)
  }

  private fun player(uuid: UUID, name: String, buffer: Double, ping: Int): SlothPlayer {
    val check = mockk<AiCheck>()
    every { check.buffer } returns buffer
    val bukkitPlayer = mockk<Player>()
    every { bukkitPlayer.name } returns name
    every { bukkitPlayer.ping } returns ping
    val checkManager = mockk<CheckManager>()
    every { checkManager.getCheck(AiCheck::class.java) } returns check
    val slothPlayer = mockk<SlothPlayer>()
    every { slothPlayer.checkManager } returns checkManager
    every { slothPlayer.uuid } returns uuid
    every { slothPlayer.player } returns bukkitPlayer
    return slothPlayer
  }

  private fun service(
    yaml: String,
    redis: RedisManager,
    scheduler: SchedulerService,
    playerData: PlayerDataManager,
  ): CrossServerSuspiciousService {
    val configManager = mockk<ConfigManager>()
    val loader = YamlConfigurationLoader.builder().source { yaml.reader().buffered() }.build()
    every { configManager.config } returns ConfigView(loader.load())
    return CrossServerSuspiciousService(
      configManager,
      redis,
      playerData,
      scheduler,
      Logger.getLogger("suspicious-test"),
    )
  }
}
