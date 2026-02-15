/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2026 KaelusMC
 *
 * This file contains code derived from GrimAC.
 * The original authors of GrimAC are credited below.
 *
 * Copyright (c) 2021-2026 GrimAC, DefineOutside and contributors.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package space.kaelus.sloth.player

import com.github.retrooper.packetevents.protocol.player.ClientVersion
import com.github.retrooper.packetevents.protocol.player.GameMode
import com.github.retrooper.packetevents.protocol.player.User
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag
import com.github.retrooper.packetevents.util.Vector3d
import java.util.Queue
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.alert.AlertManager
import space.kaelus.sloth.api.event.SlothEventBus
import space.kaelus.sloth.checks.CheckManager
import space.kaelus.sloth.checks.impl.ai.DataCollectorManager
import space.kaelus.sloth.entity.CompensatedEntities
import space.kaelus.sloth.player.state.CombatState
import space.kaelus.sloth.player.state.MovementState
import space.kaelus.sloth.player.state.TransactionTracker
import space.kaelus.sloth.punishment.PunishmentManager
import space.kaelus.sloth.scheduler.SchedulerService
import space.kaelus.sloth.server.AIServerProvider
import space.kaelus.sloth.utils.data.HeadRotation
import space.kaelus.sloth.utils.data.PacketStateData
import space.kaelus.sloth.utils.latency.ILatencyUtils
import space.kaelus.sloth.utils.latency.LatencyUtils
import space.kaelus.sloth.utils.update.RotationUpdate

class SlothPlayer
@Suppress("LongParameterList")
constructor(
  val player: Player,
  val user: User,
  private val plugin: SlothAC,
  aiSequence: Int,
  alertManager: AlertManager,
  dataCollectorManager: DataCollectorManager,
  aiServerProvider: AIServerProvider,
  val exemptManager: ExemptManager,
  private val scheduler: SchedulerService,
  checkManagerFactory: CheckManager.Factory,
  punishmentManagerFactory: PunishmentManager.Factory,
  val eventBus: SlothEventBus,
) {
  val uuid: UUID = player.uniqueId
  val packetStateData: PacketStateData = PacketStateData()
  val rotationUpdate: RotationUpdate = RotationUpdate(HeadRotation(), HeadRotation(), 0f, 0f)
  val joinTime: Long = System.currentTimeMillis()

  var entityId: Int = 0
  var gameMode: GameMode = GameMode.SURVIVAL
  var brand: String = "vanilla"

  val movement: MovementState = MovementState()
  val combat: CombatState = CombatState(aiSequence + 1)
  val transactions: TransactionTracker = TransactionTracker()

  val pendingTeleports: Queue<TeleportData> = ConcurrentLinkedQueue()
  val pendingRotations: Queue<RotationData> = ConcurrentLinkedQueue()

  val compensatedEntities: CompensatedEntities = CompensatedEntities(this)
  val latencyUtils: ILatencyUtils = LatencyUtils(this, plugin)
  val checkManager: CheckManager = checkManagerFactory.create(this)
  val punishmentManager: PunishmentManager = punishmentManagerFactory.create(this)

  fun isPointThree(): Boolean = user.clientVersion.isOlderThan(ClientVersion.V_1_18_2)

  fun getMovementThreshold(): Double = if (isPointThree()) 0.03 else 0.0002

  fun isCancelDuplicatePacket(): Boolean = true

  fun sendTransaction() {
    transactions.sendTransaction(user)
  }

  fun disconnect(reason: Component) {
    user.sendPacket(
      com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect(reason)
    )
    user.closeConnection()

    scheduler.runSync(player) { player.kick(reason) }
  }

  fun reload() {
    punishmentManager.reload()
    checkManager.reloadChecks()
  }

  class TeleportData(val location: Vector3d, val flags: RelativeFlag, val transactionId: Int) {
    fun isRelativeX(): Boolean = flags.has(RelativeFlag.X)

    fun isRelativeY(): Boolean = flags.has(RelativeFlag.Y)

    fun isRelativeZ(): Boolean = flags.has(RelativeFlag.Z)
  }

  class RotationData(val yaw: Float, val pitch: Float, val transactionId: Int)
}
