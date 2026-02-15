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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package space.kaelus.sloth.packet

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.event.UserDisconnectEvent
import com.github.retrooper.packetevents.event.UserLoginEvent
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.ClientVersion
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag
import com.github.retrooper.packetevents.protocol.world.Location
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerRotation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPainting
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation
import org.bukkit.entity.Player
import space.kaelus.sloth.player.PlayerDataManager
import space.kaelus.sloth.player.SlothPlayer
import space.kaelus.sloth.player.TransactionStamp
import space.kaelus.sloth.utils.update.RotationUpdate

class PacketListener(private val playerDataManager: PlayerDataManager) : PacketListenerAbstract() {
  override fun onUserLogin(event: UserLoginEvent) {
    val user: com.github.retrooper.packetevents.protocol.player.User? = event.user
    if (user == null) {
      return
    }
    val player: Player? = event.getPlayer()
    if (player == null) {
      return
    }
    playerDataManager.handleUserLogin(user, player)
  }

  override fun onUserDisconnect(event: UserDisconnectEvent) {
    playerDataManager.handleUserDisconnect(event.user)
  }

  private fun checkTeleportQueue(
    player: SlothPlayer,
    flying: WrapperPlayClientPlayerFlying,
  ): Boolean {
    if (!flying.hasPositionChanged() || player.pendingTeleports.isEmpty()) {
      return false
    }

    val movement = player.movement
    while (true) {
      val teleport = player.pendingTeleports.peek() ?: break
      if (player.transactions.lastTransactionReceived.get() < teleport.transactionId) {
        break
      }

      val flyingLocation = flying.location
      val flags = teleport.flags

      val expectedX =
        if (flags.has(RelativeFlag.X)) {
          movement.x + teleport.location.x
        } else {
          teleport.location.x
        }
      val expectedY =
        if (flags.has(RelativeFlag.Y)) {
          movement.y + teleport.location.y
        } else {
          teleport.location.y
        }
      val expectedZ =
        if (flags.has(RelativeFlag.Z)) {
          movement.z + teleport.location.z
        } else {
          teleport.location.z
        }

      val epsilon = 1.0E-7
      if (
        kotlin.math.abs(flyingLocation.x - expectedX) < epsilon &&
          kotlin.math.abs(flyingLocation.y - expectedY) < epsilon &&
          kotlin.math.abs(flyingLocation.z - expectedZ) < epsilon
      ) {
        player.pendingTeleports.poll()
        return true
      }

      if (player.transactions.lastTransactionReceived.get() > teleport.transactionId) {
        player.pendingTeleports.poll()
        continue
      }
      break
    }
    return false
  }

  private fun checkRotationQueue(
    player: SlothPlayer,
    flying: WrapperPlayClientPlayerFlying,
  ): Boolean {
    if (
      !flying.hasRotationChanged() ||
        flying.hasPositionChanged() ||
        player.pendingRotations.isEmpty()
    ) {
      return false
    }

    while (true) {
      val rotation = player.pendingRotations.peek() ?: break
      if (player.transactions.lastTransactionReceived.get() < rotation.transactionId) {
        break
      }

      if (flying.location.yaw == rotation.yaw && flying.location.pitch == rotation.pitch) {
        player.pendingRotations.poll()
        return true
      }

      if (player.transactions.lastTransactionReceived.get() > rotation.transactionId) {
        player.pendingRotations.poll()
        continue
      }
      break
    }
    return false
  }

  override fun onPacketReceive(event: PacketReceiveEvent) {
    val player: Player? = event.getPlayer<Player>()
    if (player == null) {
      return
    }

    val slothPlayer = playerDataManager.getPlayer(player) ?: return

    if (handleTransaction(event, slothPlayer)) {
      return
    }

    if (WrapperPlayClientPlayerFlying.isFlying(event.packetType)) {
      handleFlying(event, slothPlayer)
    }

    if (event.isCancelled) {
      resetFlags(slothPlayer)
      return
    }

    if (
      slothPlayer.packetStateData.lastPacketWasTeleport ||
        slothPlayer.packetStateData.lastPacketWasServerRotation
    ) {
      updatePlayerState(slothPlayer, WrapperPlayClientPlayerFlying(event))
    }

    slothPlayer.checkManager.onPacketReceive(event)

    resetFlags(slothPlayer)
  }

  private fun handleTransaction(event: PacketReceiveEvent, slothPlayer: SlothPlayer): Boolean {
    if (event.packetType == PacketType.Play.Client.WINDOW_CONFIRMATION) {
      val transaction = WrapperPlayClientWindowConfirmation(event)
      val id = transaction.actionId
      if (id <= 0 && addTransactionResponse(slothPlayer, id)) {
        event.isCancelled = true
      }
      return true
    } else if (event.packetType == PacketType.Play.Client.PONG) {
      val pong = WrapperPlayClientPong(event)
      val id = pong.id.toShort()
      if (addTransactionResponse(slothPlayer, id)) {
        event.isCancelled = true
      }
      return true
    }
    return false
  }

  private fun handleFlying(event: PacketReceiveEvent, slothPlayer: SlothPlayer) {
    val flying = WrapperPlayClientPlayerFlying(event)

    val teleported = checkTeleportQueue(slothPlayer, flying)
    val serverRotated = !teleported && checkRotationQueue(slothPlayer, flying)

    slothPlayer.packetStateData.lastPacketWasTeleport = teleported
    slothPlayer.packetStateData.lastPacketWasServerRotation = serverRotated

    isMojangStupid(slothPlayer, flying, event)

    if (!event.isCancelled) {
      processRotation(slothPlayer, flying)
    }
  }

  private fun processRotation(slothPlayer: SlothPlayer, packet: WrapperPlayClientPlayerFlying) {
    val ignoreRotation =
      slothPlayer.packetStateData.lastPacketWasOnePointSeventeenDuplicate &&
        slothPlayer.packetStateData.ignoreDuplicatePacketRotation
    val movement = slothPlayer.movement

    if (packet.hasPositionChanged()) {
      movement.x = packet.location.x
      movement.y = packet.location.y
      movement.z = packet.location.z
      slothPlayer.packetStateData.lastClaimedPosition = packet.location.position
    }

    if (packet.hasRotationChanged() && !ignoreRotation) {
      val newYaw = packet.location.yaw
      val newPitch = packet.location.pitch
      val deltaYaw = newYaw - movement.yaw
      val deltaPitch = newPitch - movement.pitch

      val update: RotationUpdate = slothPlayer.rotationUpdate

      update.from.yaw = movement.yaw
      update.from.pitch = movement.pitch
      update.to.yaw = newYaw
      update.to.pitch = newPitch
      update.deltaYaw = deltaYaw
      update.deltaPitch = deltaPitch

      slothPlayer.checkManager.onRotationUpdate(update)

      movement.lastYaw = movement.yaw
      movement.lastPitch = movement.pitch
      movement.yaw = newYaw
      movement.pitch = newPitch
    }
  }

  private fun updatePlayerState(slothPlayer: SlothPlayer, flying: WrapperPlayClientPlayerFlying) {
    val movement = slothPlayer.movement
    if (flying.hasPositionChanged()) {
      movement.x = flying.location.x
      movement.y = flying.location.y
      movement.z = flying.location.z
    }
    if (flying.hasRotationChanged()) {
      movement.yaw = flying.location.yaw
      movement.pitch = flying.location.pitch
    }
  }

  private fun resetFlags(slothPlayer: SlothPlayer) {
    slothPlayer.packetStateData.lastPacketWasOnePointSeventeenDuplicate = false
    slothPlayer.packetStateData.lastPacketWasTeleport = false
    slothPlayer.packetStateData.lastPacketWasServerRotation = false
  }

  override fun onPacketSend(event: PacketSendEvent) {
    val player: Player? = event.getPlayer<Player>()
    if (player == null) {
      return
    }

    val slothPlayer = playerDataManager.getPlayer(player) ?: return

    val packetType = event.packetType as PacketType.Play.Server

    when (packetType) {
      PacketType.Play.Server.WINDOW_CONFIRMATION ->
        handleWindowConfirmation(WrapperPlayServerWindowConfirmation(event), slothPlayer)
      PacketType.Play.Server.PING -> handlePing(WrapperPlayServerPing(event), slothPlayer)
      PacketType.Play.Server.SPAWN_ENTITY ->
        handleSpawnEntity(WrapperPlayServerSpawnEntity(event), slothPlayer)
      PacketType.Play.Server.SPAWN_LIVING_ENTITY ->
        handleSpawnLivingEntity(WrapperPlayServerSpawnLivingEntity(event), slothPlayer)
      PacketType.Play.Server.SPAWN_PAINTING ->
        handleSpawnPainting(WrapperPlayServerSpawnPainting(event), slothPlayer)
      PacketType.Play.Server.SPAWN_PLAYER ->
        handleSpawnPlayer(WrapperPlayServerSpawnPlayer(event), slothPlayer)
      PacketType.Play.Server.DESTROY_ENTITIES ->
        handleDestroyEntities(WrapperPlayServerDestroyEntities(event), slothPlayer)
      PacketType.Play.Server.JOIN_GAME ->
        handleJoinGame(WrapperPlayServerJoinGame(event), slothPlayer)
      PacketType.Play.Server.RESPAWN -> handleRespawn(slothPlayer)
      PacketType.Play.Server.PLAYER_POSITION_AND_LOOK ->
        handlePositionAndLook(WrapperPlayServerPlayerPositionAndLook(event), slothPlayer)
      PacketType.Play.Server.PLAYER_ROTATION ->
        handlePlayerRotation(WrapperPlayServerPlayerRotation(event), slothPlayer)
      else -> Unit
    }
  }

  private fun handleWindowConfirmation(
    confirmation: WrapperPlayServerWindowConfirmation,
    slothPlayer: SlothPlayer,
  ) {
    val id = confirmation.actionId
    val transactions = slothPlayer.transactions
    if (id <= 0 && transactions.didWeSendThatTrans.remove(id)) {
      transactions.entitiesDespawnedThisTransaction.clear()
      transactions.transactionsSent.add(TransactionStamp(id, System.nanoTime()))
      transactions.lastTransactionSent.getAndIncrement()
    }
  }

  private fun handlePing(ping: WrapperPlayServerPing, slothPlayer: SlothPlayer) {
    val id = ping.id
    val transactions = slothPlayer.transactions
    if (id == id.toShort().toInt() && transactions.didWeSendThatTrans.remove(id.toShort())) {
      transactions.entitiesDespawnedThisTransaction.clear()
      transactions.transactionsSent.add(TransactionStamp(id.toShort(), System.nanoTime()))
      transactions.lastTransactionSent.getAndIncrement()
    }
  }

  private fun handleSpawnEntity(spawn: WrapperPlayServerSpawnEntity, slothPlayer: SlothPlayer) {
    if (slothPlayer.transactions.entitiesDespawnedThisTransaction.contains(spawn.entityId)) {
      slothPlayer.sendTransaction()
    }
    slothPlayer.latencyUtils.addRealTimeTask(
      slothPlayer.transactions.lastTransactionSent.get(),
      Runnable {
        slothPlayer.compensatedEntities.addEntity(
          spawn.entityId,
          spawn.uuid.orElse(null),
          spawn.entityType,
        )
      },
    )
  }

  private fun handleSpawnLivingEntity(
    spawn: WrapperPlayServerSpawnLivingEntity,
    slothPlayer: SlothPlayer,
  ) {
    if (slothPlayer.transactions.entitiesDespawnedThisTransaction.contains(spawn.entityId)) {
      slothPlayer.sendTransaction()
    }
    slothPlayer.latencyUtils.addRealTimeTask(
      slothPlayer.transactions.lastTransactionSent.get(),
      Runnable {
        slothPlayer.compensatedEntities.addEntity(
          spawn.entityId,
          spawn.entityUUID,
          spawn.entityType,
        )
      },
    )
  }

  private fun handleSpawnPainting(spawn: WrapperPlayServerSpawnPainting, slothPlayer: SlothPlayer) {
    if (slothPlayer.transactions.entitiesDespawnedThisTransaction.contains(spawn.entityId)) {
      slothPlayer.sendTransaction()
    }
    slothPlayer.latencyUtils.addRealTimeTask(
      slothPlayer.transactions.lastTransactionSent.get(),
      Runnable {
        slothPlayer.compensatedEntities.addEntity(spawn.entityId, spawn.uuid, EntityTypes.PAINTING)
      },
    )
  }

  private fun handleSpawnPlayer(spawn: WrapperPlayServerSpawnPlayer, slothPlayer: SlothPlayer) {
    if (slothPlayer.transactions.entitiesDespawnedThisTransaction.contains(spawn.entityId)) {
      slothPlayer.sendTransaction()
    }
    slothPlayer.latencyUtils.addRealTimeTask(
      slothPlayer.transactions.lastTransactionSent.get(),
      Runnable {
        slothPlayer.compensatedEntities.addEntity(spawn.entityId, spawn.uuid, EntityTypes.PLAYER)
      },
    )
  }

  private fun handleDestroyEntities(
    destroy: WrapperPlayServerDestroyEntities,
    slothPlayer: SlothPlayer,
  ) {
    for (id in destroy.entityIds) {
      slothPlayer.transactions.entitiesDespawnedThisTransaction.add(id)
    }
    slothPlayer.latencyUtils.addRealTimeTask(
      slothPlayer.transactions.lastTransactionSent.get() + 1,
      Runnable {
        for (id in destroy.entityIds) {
          slothPlayer.compensatedEntities.removeEntity(id)
        }
      },
    )
  }

  private fun handleJoinGame(join: WrapperPlayServerJoinGame, slothPlayer: SlothPlayer) {
    slothPlayer.latencyUtils.addRealTimeTask(
      slothPlayer.transactions.lastTransactionSent.get(),
      Runnable {
        slothPlayer.entityId = join.entityId
        slothPlayer.gameMode = join.gameMode
        slothPlayer.compensatedEntities.clear()
      },
    )
  }

  private fun handleRespawn(slothPlayer: SlothPlayer) {
    slothPlayer.latencyUtils.addRealTimeTask(
      slothPlayer.transactions.lastTransactionSent.get(),
      Runnable { slothPlayer.compensatedEntities.clear() },
    )
  }

  private fun handlePositionAndLook(
    wrapper: WrapperPlayServerPlayerPositionAndLook,
    slothPlayer: SlothPlayer,
  ) {
    slothPlayer.sendTransaction()
    val transactionId = slothPlayer.transactions.lastTransactionSent.get()
    val location = Vector3d(wrapper.x, wrapper.y, wrapper.z)
    val flags = wrapper.relativeFlags
    slothPlayer.pendingTeleports.add(SlothPlayer.TeleportData(location, flags, transactionId))
  }

  private fun handlePlayerRotation(
    wrapper: WrapperPlayServerPlayerRotation,
    slothPlayer: SlothPlayer,
  ) {
    slothPlayer.sendTransaction()
    val transactionId = slothPlayer.transactions.lastTransactionSent.get()
    slothPlayer.pendingRotations.add(
      SlothPlayer.RotationData(wrapper.yaw, wrapper.pitch, transactionId)
    )
  }

  private fun addTransactionResponse(player: SlothPlayer, id: Short): Boolean {
    var data: TransactionStamp? = null
    var hasId = false

    for (entry in player.transactions.transactionsSent) {
      if (entry.id == id) {
        hasId = true
        break
      }
    }

    if (hasId) {
      do {
        data = player.transactions.transactionsSent.poll()
        if (data == null) break
        player.transactions.lastTransactionReceived.incrementAndGet()
      } while (data.id != id)

      player.latencyUtils.handleNettySyncTransaction(
        player.transactions.lastTransactionReceived.get()
      )
    }
    return data != null
  }

  private fun isMojangStupid(
    player: SlothPlayer,
    flying: WrapperPlayClientPlayerFlying,
    event: PacketReceiveEvent,
  ) {
    if (player.packetStateData.lastPacketWasTeleport) return
    if (player.user.clientVersion.isNewerThanOrEquals(ClientVersion.V_1_21)) return

    val location: Location = flying.location
    val movement = player.movement
    val threshold = player.getMovementThreshold()
    val inVehicle = player.compensatedEntities.self.riding != null

    if (
      !player.packetStateData.lastPacketWasTeleport &&
        flying.hasPositionChanged() &&
        flying.hasRotationChanged() &&
        ((flying.isOnGround == player.packetStateData.packetPlayerOnGround &&
          (player.user.clientVersion.isNewerThanOrEquals(ClientVersion.V_1_17) &&
            player.packetStateData.lastClaimedPosition.distanceSquared(location.position) <
              threshold * threshold)) || inVehicle)
    ) {
      if (player.isCancelDuplicatePacket()) {
        event.isCancelled = true
      }

      player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = true

      if (!player.packetStateData.ignoreDuplicatePacketRotation) {
        if (movement.yaw != location.yaw || movement.pitch != location.pitch) {
          movement.lastYaw = movement.yaw
          movement.lastPitch = movement.pitch
        }
        movement.yaw = location.yaw
        movement.pitch = location.pitch
      }

      player.packetStateData.lastClaimedPosition = location.position
    }
  }
}
