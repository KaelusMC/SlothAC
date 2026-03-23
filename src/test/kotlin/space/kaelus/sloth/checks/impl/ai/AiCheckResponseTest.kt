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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.lang.reflect.Method
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.assertEquals
import org.bukkit.entity.Player
import org.junit.jupiter.api.Test
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.ai.AiResult
import space.kaelus.sloth.ai.AiService
import space.kaelus.sloth.alert.AlertManager
import space.kaelus.sloth.api.event.SlothEventBus
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.damage.DamageProcessor
import space.kaelus.sloth.debug.DebugCategory
import space.kaelus.sloth.debug.DebugManager
import space.kaelus.sloth.player.SlothPlayer
import space.kaelus.sloth.player.state.CombatState
import space.kaelus.sloth.punishment.PunishmentManager
import space.kaelus.sloth.region.RegionProvider
import space.kaelus.sloth.scheduler.SchedulerService
import space.kaelus.sloth.server.AIResponse

class AiCheckResponseTest {

  @Test
  fun `probability debug log uses fixed formatting only when enabled`() {
    val fixture =
      createFixture(debugEnabled = true, enabledCategories = setOf(DebugCategory.AI_PROBABILITY))

    fixture.invokeOnResponse(0.42)

    verify(exactly = 1) {
      fixture.logger.info(
        "[DEBUG | AI_PROBABILITY] [TestPlayer] Prob: 0.4200 | Buffer: 0.00 -> 0.00 | Damage Multiplier: 1.00"
      )
    }
  }

  @Test
  fun `flag debug string uses compact fixed formatting`() {
    val fixture = createFixture(aiFlag = 0.5, bufferMultiplier = 20.0)

    fixture.invokeOnResponse(0.95)

    verify(exactly = 1) {
      fixture.punishmentManager.handleFlag(fixture.check, "prob=0.95 buffer=1.0")
    }
    assertEquals(0.0, fixture.check.buffer)
  }

  @Test
  fun `ai formatting helpers use stable decimal output`() {
    assertEquals("1.0", formatAiBuffer(1.04))
    assertEquals("0.95", formatAiProbability(0.945))
    assertEquals("0.4200", formatAiProbabilityVerbose(0.42))
  }

  private fun createFixture(
    debugEnabled: Boolean = false,
    enabledCategories: Set<DebugCategory> = emptySet(),
    aiFlag: Double = 10.0,
    bufferMultiplier: Double = 1.0,
  ): Fixture {
    val logger = mockk<Logger>(relaxed = true)
    val plugin = mockk<SlothAC>(relaxed = true)
    every { plugin.logger } returns logger

    val aiService = mockk<AiService>(relaxed = true)
    every { aiService.isEnabled } returns true

    val configManager = mockk<ConfigManager>(relaxed = true)
    every { configManager.aiSequence } returns 40
    every { configManager.aiStep } returns 1
    every { configManager.aiFlag } returns aiFlag
    every { configManager.aiResetOnFlag } returns 0.0
    every { configManager.aiBufferMultiplier } returns bufferMultiplier
    every { configManager.aiBufferDecrease } returns 0.25
    every { configManager.suspiciousAlertsBuffer } returns 25.0
    every { configManager.enabledDebugCategories } returns enabledCategories
    every { configManager.isDebugEnabled() } returns debugEnabled

    val player = mockk<Player>(relaxed = true)
    every { player.name } returns "TestPlayer"
    every { player.uniqueId } returns UUID.fromString("00000000-0000-0000-0000-000000000001")

    val eventBus = mockk<SlothEventBus>(relaxed = true)
    val punishmentManager = mockk<PunishmentManager>(relaxed = true)
    val combat = CombatState(0)

    val slothPlayer = mockk<SlothPlayer>(relaxed = true)
    every { slothPlayer.player } returns player
    every { slothPlayer.uuid } returns player.uniqueId
    every { slothPlayer.eventBus } returns eventBus
    every { slothPlayer.punishmentManager } returns punishmentManager
    every { slothPlayer.combat } returns combat

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
        scheduler = mockk<SchedulerService>(relaxed = true),
      )

    return Fixture(check, logger, punishmentManager)
  }

  private data class Fixture(
    val check: AiCheck,
    val logger: Logger,
    val punishmentManager: PunishmentManager,
  ) {
    fun invokeOnResponse(probability: Double) {
      onResponseMethod.invoke(
        check,
        AiResult(AIResponse(probability), """{"probability":$probability}""", null, false),
      )
    }
  }

  private companion object {
    val onResponseMethod: Method =
      AiCheck::class.java.getDeclaredMethod("onResponse", AiResult::class.java).apply {
        isAccessible = true
      }
  }
}
