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
package space.kaelus.sloth.checks.impl.ai

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger
import org.bukkit.entity.Player
import org.junit.jupiter.api.Test
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.ai.AiResult
import space.kaelus.sloth.ai.AiService
import space.kaelus.sloth.alert.AlertManager
import space.kaelus.sloth.checks.CheckManager
import space.kaelus.sloth.checks.impl.combat.AimProcessor
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.damage.DamageProcessor
import space.kaelus.sloth.debug.DebugManager
import space.kaelus.sloth.entity.CompensatedEntities
import space.kaelus.sloth.entity.PacketEntity
import space.kaelus.sloth.entity.types.PacketEntitySelf
import space.kaelus.sloth.platform.scheduler.TaskHandle
import space.kaelus.sloth.player.SlothPlayer
import space.kaelus.sloth.player.state.CombatState
import space.kaelus.sloth.player.state.MovementState
import space.kaelus.sloth.region.RegionProvider
import space.kaelus.sloth.scheduler.SchedulerService
import space.kaelus.sloth.utils.data.PacketStateData

class AiCheckRidingExemptionTest {

  @Test
  fun `riding player is not sent to inference`() {
    val fixture = createFixture(riding = true)

    repeat(SEQUENCE) { fixture.check.onPacketReceive(fixture.event) }

    verify(exactly = 0) { fixture.aiService.request(any(), any()) }
  }

  @Test
  fun `player on foot is sent to inference`() {
    val fixture = createFixture(riding = false)

    repeat(SEQUENCE) { fixture.check.onPacketReceive(fixture.event) }

    verify(exactly = 1) { fixture.aiService.request(any(), any()) }
  }

  @Test
  fun `mount mid-window clears accumulated ticks`() {
    val fixture = createFixture(riding = false)

    repeat(SEQUENCE - 1) { fixture.check.onPacketReceive(fixture.event) }
    fixture.ridingHolder[0] = mockk(relaxed = true)
    fixture.check.onPacketReceive(fixture.event)
    fixture.ridingHolder[0] = null
    repeat(SEQUENCE - 1) { fixture.check.onPacketReceive(fixture.event) }

    verify(exactly = 0) { fixture.aiService.request(any(), any()) }

    fixture.check.onPacketReceive(fixture.event)

    verify(exactly = 1) { fixture.aiService.request(any(), any()) }
  }

  private fun createFixture(riding: Boolean): Fixture {
    val logger = mockk<Logger>(relaxed = true)
    val plugin = mockk<SlothAC>(relaxed = true)
    every { plugin.logger } returns logger

    val aiService = mockk<AiService>(relaxed = true)
    every { aiService.isEnabled } returns true
    every { aiService.request(any(), any()) } returns CompletableFuture<AiResult>()

    val configManager = mockk<ConfigManager>(relaxed = true)
    every { configManager.aiSequence } returns SEQUENCE
    every { configManager.aiStep } returns 1
    every { configManager.aiContinuous } returns false
    every { configManager.isAiWorldGuardEnabled() } returns false
    every { configManager.isDebugEnabled() } returns false

    val packetStateData = PacketStateData()
    val player = mockk<Player>(relaxed = true)
    every { player.name } returns "TestPlayer"

    val aimProcessor = mockk<AimProcessor>(relaxed = true)
    val checkManager = mockk<CheckManager>(relaxed = true)
    every { checkManager.getCheck(AimProcessor::class.java) } returns aimProcessor

    val ridingHolder = arrayOfNulls<PacketEntity>(1)
    if (riding) {
      ridingHolder[0] = mockk(relaxed = true)
    }
    val self = mockk<PacketEntitySelf>(relaxed = true)
    every { self.riding } answers { ridingHolder[0] }
    val compensatedEntities = mockk<CompensatedEntities>(relaxed = true)
    every { compensatedEntities.self } returns self

    val slothPlayer = mockk<SlothPlayer>(relaxed = true)
    every { slothPlayer.player } returns player
    every { slothPlayer.packetStateData } returns packetStateData
    every { slothPlayer.checkManager } returns checkManager
    every { slothPlayer.compensatedEntities } returns compensatedEntities
    every { slothPlayer.movement } returns MovementState()
    every { slothPlayer.combat } returns CombatState(0)

    val scheduler = mockk<SchedulerService>(relaxed = true)
    every { scheduler.runAsync(any()) } answers
      {
        firstArg<Runnable>().run()
        mockk<TaskHandle>(relaxed = true)
      }

    val event = mockk<PacketReceiveEvent>(relaxed = true)
    every { event.packetType } returns PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION

    val check =
      AiCheck(
        slothPlayer = slothPlayer,
        plugin = plugin,
        aiService = aiService,
        configManager = configManager,
        regionProvider = mockk<RegionProvider>(relaxed = true),
        alertManager = mockk<AlertManager>(relaxed = true),
        damageProcessor = mockk<DamageProcessor>(relaxed = true),
        debugManager = DebugManager(plugin, configManager),
        scheduler = scheduler,
      )

    return Fixture(check, aiService, event, ridingHolder)
  }

  private data class Fixture(
    val check: AiCheck,
    val aiService: AiService,
    val event: PacketReceiveEvent,
    val ridingHolder: Array<PacketEntity?>,
  )

  private companion object {
    private const val SEQUENCE = 4
  }
}
