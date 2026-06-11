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
import java.util.UUID
import java.util.logging.Logger
import org.bukkit.entity.Player
import org.junit.jupiter.api.Test
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.checks.CheckManager
import space.kaelus.sloth.checks.impl.combat.AimProcessor
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.data.DataSession
import space.kaelus.sloth.entity.CompensatedEntities
import space.kaelus.sloth.entity.types.PacketEntitySelf
import space.kaelus.sloth.player.SlothPlayer
import space.kaelus.sloth.utils.data.PacketStateData

class DataCollectorCheckTest {

  @Test
  fun `duplicate flying packet does not enter data collection`() {
    val fixture = createFixture()
    fixture.packetStateData.lastPacketWasOnePointSeventeenDuplicate = true

    fixture.check.onPacketReceive(fixture.event)

    verify(exactly = 0) { fixture.session.addTick(any()) }
  }

  @Test
  fun `riding player does not enter data collection`() {
    val fixture = createFixture(riding = true)

    fixture.check.onPacketReceive(fixture.event)

    verify(exactly = 0) { fixture.session.addTick(any()) }
  }

  @Test
  fun `normal flying packet enters data collection`() {
    val fixture = createFixture()

    fixture.check.onPacketReceive(fixture.event)

    verify(exactly = 1) { fixture.session.addTick(any()) }
  }

  private fun createFixture(riding: Boolean = false): Fixture {
    val logger = mockk<Logger>(relaxed = true)
    val plugin = mockk<SlothAC>(relaxed = true)
    every { plugin.logger } returns logger

    val configManager = mockk<ConfigManager>(relaxed = true)
    every { configManager.aiContinuous } returns true

    val packetStateData = PacketStateData()
    val player = mockk<Player>(relaxed = true)
    every { player.name } returns "TestPlayer"

    val aimProcessor = mockk<AimProcessor>(relaxed = true)
    val checkManager = mockk<CheckManager>(relaxed = true)
    every { checkManager.getCheck(AimProcessor::class.java) } returns aimProcessor

    val self = mockk<PacketEntitySelf>(relaxed = true)
    every { self.riding } returns if (riding) mockk(relaxed = true) else null
    val compensatedEntities = mockk<CompensatedEntities>(relaxed = true)
    every { compensatedEntities.self } returns self

    val uuid = UUID.randomUUID()
    val slothPlayer = mockk<SlothPlayer>(relaxed = true)
    every { slothPlayer.player } returns player
    every { slothPlayer.uuid } returns uuid
    every { slothPlayer.packetStateData } returns packetStateData
    every { slothPlayer.checkManager } returns checkManager
    every { slothPlayer.compensatedEntities } returns compensatedEntities

    val session = mockk<DataSession>(relaxed = true)
    val dataCollectorManager = mockk<DataCollectorManager>(relaxed = true)
    every { dataCollectorManager.getSession(uuid) } returns session

    val event = mockk<PacketReceiveEvent>(relaxed = true)
    every { event.packetType } returns PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION

    val check =
      DataCollectorCheck(
        slothPlayer = slothPlayer,
        dataCollectorManager = dataCollectorManager,
        plugin = plugin,
        configManager = configManager,
      )

    return Fixture(check, session, event, packetStateData)
  }

  private data class Fixture(
    val check: DataCollectorCheck,
    val session: DataSession,
    val event: PacketReceiveEvent,
    val packetStateData: PacketStateData,
  )
}
