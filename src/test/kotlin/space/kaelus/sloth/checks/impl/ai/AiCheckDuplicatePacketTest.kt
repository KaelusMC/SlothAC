package space.kaelus.sloth.checks.impl.ai

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.lang.reflect.Field
import java.util.ArrayDeque
import java.util.logging.Logger
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.ai.AiService
import space.kaelus.sloth.alert.AlertManager
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.damage.DamageProcessor
import space.kaelus.sloth.data.TickData
import space.kaelus.sloth.debug.DebugCategory
import space.kaelus.sloth.debug.DebugManager
import space.kaelus.sloth.player.SlothPlayer
import space.kaelus.sloth.region.RegionProvider
import space.kaelus.sloth.scheduler.SchedulerService
import space.kaelus.sloth.utils.data.PacketStateData

class AiCheckDuplicatePacketTest {

  @Test
  fun `duplicate flying packet is ignored before ai request`() {
    val fixture = createFixture()

    fixture.check.onPacketReceive(fixture.event)

    verify(exactly = 0) { fixture.aiService.request(any(), any()) }
  }

  @Test
  fun `duplicate flying packet does not log to console when debug is disabled`() {
    val fixture = createFixture()

    fixture.check.onPacketReceive(fixture.event)

    verify(exactly = 0) { fixture.logger.info(any<String>()) }
  }

  @Test
  fun `duplicate flying packet does not enter ai tick sequence`() {
    val fixture = createFixture()

    fixture.check.onPacketReceive(fixture.event)

    assertEquals(0, fixture.tickSequenceSize())
  }

  @Test
  fun `duplicate flying packet logs only when packet duplication debug is enabled`() {
    val fixture =
      createFixture(
        debugEnabled = true,
        enabledCategories = setOf(DebugCategory.PACKET_DUPLICATION),
      )

    fixture.check.onPacketReceive(fixture.event)

    verify(exactly = 1) {
      fixture.logger.info(
        match<String> { message ->
          message.contains("[DEBUG | PACKET_DUPLICATION]") &&
            message.contains("Mojang failed IQ Test for: TestPlayer.")
        }
      )
    }
  }

  private fun createFixture(
    debugEnabled: Boolean = false,
    enabledCategories: Set<DebugCategory> = emptySet(),
  ): Fixture {
    val logger = mockk<Logger>(relaxed = true)
    val plugin = mockk<SlothAC>(relaxed = true)
    every { plugin.logger } returns logger

    val aiService = mockk<AiService>(relaxed = true)
    every { aiService.isEnabled } returns true

    val configManager = mockk<ConfigManager>(relaxed = true)
    every { configManager.aiSequence } returns 40
    every { configManager.aiStep } returns 1
    every { configManager.aiFlag } returns 1.0
    every { configManager.aiResetOnFlag } returns 0.0
    every { configManager.aiBufferMultiplier } returns 1.0
    every { configManager.aiBufferDecrease } returns 0.25
    every { configManager.suspiciousAlertsBuffer } returns 25.0
    every { configManager.enabledDebugCategories } returns enabledCategories
    every { configManager.isDebugEnabled() } returns debugEnabled

    val packetStateData = PacketStateData().apply { lastPacketWasOnePointSeventeenDuplicate = true }
    val player = mockk<Player>(relaxed = true)
    every { player.name } returns "TestPlayer"

    val slothPlayer = mockk<SlothPlayer>(relaxed = true)
    every { slothPlayer.player } returns player
    every { slothPlayer.packetStateData } returns packetStateData

    val event = mockk<PacketReceiveEvent>(relaxed = true)
    every { event.packetType } returns PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION

    val debugManager = DebugManager(plugin, configManager)
    val check =
      AiCheck(
        slothPlayer = slothPlayer,
        plugin = plugin,
        aiService = aiService,
        configManager = configManager,
        regionProvider = mockk<RegionProvider>(relaxed = true),
        alertManager = mockk<AlertManager>(relaxed = true),
        damageProcessor = mockk<DamageProcessor>(relaxed = true),
        debugManager = debugManager,
        scheduler = mockk<SchedulerService>(relaxed = true),
      )

    return Fixture(check, aiService, event, logger)
  }

  private data class Fixture(
    val check: AiCheck,
    val aiService: AiService,
    val event: PacketReceiveEvent,
    val logger: Logger,
  ) {
    fun tickSequenceSize(): Int {
      @Suppress("UNCHECKED_CAST") val ticks = ticksField.get(check) as ArrayDeque<TickData>
      return ticks.size
    }
  }

  private companion object {
    val ticksField: Field =
      AiCheck::class.java.getDeclaredField("ticks").apply { isAccessible = true }
  }
}
