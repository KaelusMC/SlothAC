/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2025 KaelusMC
 *
 * This file contains code derived from GrimAC.
 * The original authors of GrimAC are credited below.
 *
 * Copyright (c) 2021-2025 GrimAC, DefineOutside and contributors.
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
package space.kaelus.sloth.packet;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.bukkit.entity.Player;
import space.kaelus.sloth.player.PlayerDataManager;
import space.kaelus.sloth.player.SlothPlayer;
import space.kaelus.sloth.utils.data.Pair;
import space.kaelus.sloth.utils.update.RotationUpdate;

@Singleton
public class PacketListener extends PacketListenerAbstract {
  private final PlayerDataManager playerDataManager;

  @Inject
  public PacketListener(PlayerDataManager playerDataManager) {
    this.playerDataManager = playerDataManager;
  }

  private boolean checkTeleportQueue(SlothPlayer player, WrapperPlayClientPlayerFlying flying) {
    if (!flying.hasPositionChanged() || player.getPendingTeleports().isEmpty()) {
      return false;
    }

    SlothPlayer.TeleportData teleport;
    while ((teleport = player.getPendingTeleports().peek()) != null) {
      if (player.getLastTransactionReceived().get() < teleport.getTransactionId()) {
        break;
      }

      Location flyingLocation = flying.getLocation();
      RelativeFlag flags = teleport.getFlags();

      double expectedX =
          flags.has(RelativeFlag.X)
              ? player.x + teleport.getLocation().getX()
              : teleport.getLocation().getX();
      double expectedY =
          flags.has(RelativeFlag.Y)
              ? player.y + teleport.getLocation().getY()
              : teleport.getLocation().getY();
      double expectedZ =
          flags.has(RelativeFlag.Z)
              ? player.z + teleport.getLocation().getZ()
              : teleport.getLocation().getZ();

      final double epsilon = 1.0E-7;
      if (Math.abs(flyingLocation.getX() - expectedX) < epsilon
          && Math.abs(flyingLocation.getY() - expectedY) < epsilon
          && Math.abs(flyingLocation.getZ() - expectedZ) < epsilon) {

        player.getPendingTeleports().poll();
        return true;
      }

      if (player.getLastTransactionReceived().get() > teleport.getTransactionId()) {
        player.getPendingTeleports().poll();
        continue;
      }
      break;
    }
    return false;
  }

  private boolean checkRotationQueue(SlothPlayer player, WrapperPlayClientPlayerFlying flying) {
    if (!flying.hasRotationChanged()
        || flying.hasPositionChanged()
        || player.getPendingRotations().isEmpty()) {
      return false;
    }

    SlothPlayer.RotationData rotation;
    while ((rotation = player.getPendingRotations().peek()) != null) {
      if (player.getLastTransactionReceived().get() < rotation.getTransactionId()) {
        break;
      }

      if (flying.getLocation().getYaw() == rotation.getYaw()
          && flying.getLocation().getPitch() == rotation.getPitch()) {
        player.getPendingRotations().poll();
        return true;
      }

      if (player.getLastTransactionReceived().get() > rotation.getTransactionId()) {
        player.getPendingRotations().poll();
        continue;
      }
      break;
    }
    return false;
  }

  @Override
  public void onPacketReceive(PacketReceiveEvent event) {
    if (!(event.getPlayer() instanceof Player)) {
      return;
    }

    SlothPlayer slothPlayer = playerDataManager.getPlayer((Player) event.getPlayer());
    if (slothPlayer == null) return;

    if (handleTransaction(event, slothPlayer)) {
      return;
    }

    if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
      handleFlying(event, slothPlayer);
    }

    if (event.isCancelled()) {
      resetFlags(slothPlayer);
      return;
    }

    if (slothPlayer.packetStateData.lastPacketWasTeleport
        || slothPlayer.packetStateData.lastPacketWasServerRotation) {
      updatePlayerState(slothPlayer, new WrapperPlayClientPlayerFlying(event));
    }

    slothPlayer.getCheckManager().onPacketReceive(event);

    resetFlags(slothPlayer);
  }

  private boolean handleTransaction(PacketReceiveEvent event, SlothPlayer slothPlayer) {
    short id;
    if (event.getPacketType() == PacketType.Play.Client.WINDOW_CONFIRMATION) {
      WrapperPlayClientWindowConfirmation transaction =
          new WrapperPlayClientWindowConfirmation(event);
      id = transaction.getActionId();
      if (id <= 0 && addTransactionResponse(slothPlayer, id)) {
        event.setCancelled(true);
      }
      return true;
    } else if (event.getPacketType() == PacketType.Play.Client.PONG) {
      WrapperPlayClientPong pong = new WrapperPlayClientPong(event);
      id = (short) pong.getId();
      if (addTransactionResponse(slothPlayer, id)) {
        event.setCancelled(true);
      }
      return true;
    }
    return false;
  }

  private void handleFlying(PacketReceiveEvent event, SlothPlayer slothPlayer) {
    WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);

    boolean teleported = checkTeleportQueue(slothPlayer, flying);
    boolean serverRotated = !teleported && checkRotationQueue(slothPlayer, flying);

    slothPlayer.packetStateData.lastPacketWasTeleport = teleported;
    slothPlayer.packetStateData.lastPacketWasServerRotation = serverRotated;

    isMojangStupid(slothPlayer, flying, event);

    if (!event.isCancelled()) {
      processRotation(slothPlayer, flying);
    }
  }

  private void processRotation(SlothPlayer slothPlayer, WrapperPlayClientPlayerFlying packet) {
    boolean ignoreRotation =
        slothPlayer.packetStateData.lastPacketWasOnePointSeventeenDuplicate
            && slothPlayer.packetStateData.ignoreDuplicatePacketRotation;

    if (packet.hasPositionChanged()) {
      slothPlayer.x = packet.getLocation().getX();
      slothPlayer.y = packet.getLocation().getY();
      slothPlayer.z = packet.getLocation().getZ();
      slothPlayer.packetStateData.lastClaimedPosition = packet.getLocation().getPosition();
    }

    if (packet.hasRotationChanged() && !ignoreRotation) {
      float newYaw = packet.getLocation().getYaw();
      float newPitch = packet.getLocation().getPitch();
      float deltaYaw = newYaw - slothPlayer.yaw;
      float deltaPitch = newPitch - slothPlayer.pitch;

      RotationUpdate update = slothPlayer.rotationUpdate;

      update.getFrom().setYaw(slothPlayer.yaw);
      update.getFrom().setPitch(slothPlayer.pitch);
      update.getTo().setYaw(newYaw);
      update.getTo().setPitch(newPitch);
      update.setDeltaYaw(deltaYaw);
      update.setDeltaPitch(deltaPitch);

      slothPlayer.getCheckManager().onRotationUpdate(update);

      slothPlayer.lastYaw = slothPlayer.yaw;
      slothPlayer.lastPitch = slothPlayer.pitch;
      slothPlayer.yaw = newYaw;
      slothPlayer.pitch = newPitch;
    }
  }

  private void updatePlayerState(SlothPlayer slothPlayer, WrapperPlayClientPlayerFlying flying) {
    if (flying.hasPositionChanged()) {
      slothPlayer.x = flying.getLocation().getX();
      slothPlayer.y = flying.getLocation().getY();
      slothPlayer.z = flying.getLocation().getZ();
    }
    if (flying.hasRotationChanged()) {
      slothPlayer.yaw = flying.getLocation().getYaw();
      slothPlayer.pitch = flying.getLocation().getPitch();
    }
  }

  private void resetFlags(SlothPlayer slothPlayer) {
    slothPlayer.packetStateData.lastPacketWasOnePointSeventeenDuplicate = false;
    slothPlayer.packetStateData.lastPacketWasTeleport = false;
    slothPlayer.packetStateData.lastPacketWasServerRotation = false;
  }

  @Override
  public void onPacketSend(PacketSendEvent event) {
    if (!(event.getPlayer() instanceof Player)) {
      return;
    }

    SlothPlayer slothPlayer = playerDataManager.getPlayer((Player) event.getPlayer());
    if (slothPlayer == null) return;

    final PacketType.Play.Server packetType = (PacketType.Play.Server) event.getPacketType();

    if (packetType == PacketType.Play.Server.WINDOW_CONFIRMATION) {
      handleWindowConfirmation(new WrapperPlayServerWindowConfirmation(event), slothPlayer);
    } else if (packetType == PacketType.Play.Server.PING) {
      handlePing(new WrapperPlayServerPing(event), slothPlayer);
    } else if (packetType == PacketType.Play.Server.SPAWN_ENTITY) {
      handleSpawnEntity(new WrapperPlayServerSpawnEntity(event), slothPlayer);
    } else if (packetType == PacketType.Play.Server.SPAWN_LIVING_ENTITY) {
      handleSpawnLivingEntity(new WrapperPlayServerSpawnLivingEntity(event), slothPlayer);
    } else if (packetType == PacketType.Play.Server.SPAWN_PAINTING) {
      handleSpawnPainting(new WrapperPlayServerSpawnPainting(event), slothPlayer);
    } else if (packetType == PacketType.Play.Server.SPAWN_PLAYER) {
      handleSpawnPlayer(new WrapperPlayServerSpawnPlayer(event), slothPlayer);
    } else if (packetType == PacketType.Play.Server.DESTROY_ENTITIES) {
      handleDestroyEntities(new WrapperPlayServerDestroyEntities(event), slothPlayer);
    } else if (packetType == PacketType.Play.Server.JOIN_GAME) {
      handleJoinGame(new WrapperPlayServerJoinGame(event), slothPlayer);
    } else if (packetType == PacketType.Play.Server.RESPAWN) {
      handleRespawn(slothPlayer);
    } else if (packetType == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
      handlePositionAndLook(new WrapperPlayServerPlayerPositionAndLook(event), slothPlayer);
    } else if (packetType == PacketType.Play.Server.PLAYER_ROTATION) {
      handlePlayerRotation(new WrapperPlayServerPlayerRotation(event), slothPlayer);
    }
  }

  private void handleWindowConfirmation(
      WrapperPlayServerWindowConfirmation confirmation, SlothPlayer slothPlayer) {
    short id = confirmation.getActionId();
    if (id <= 0 && slothPlayer.didWeSendThatTrans.remove(id)) {
      slothPlayer.entitiesDespawnedThisTransaction.clear();
      slothPlayer.transactionsSent.add(new Pair<>(id, System.nanoTime()));
      slothPlayer.getLastTransactionSent().getAndIncrement();
    }
  }

  private void handlePing(WrapperPlayServerPing ping, SlothPlayer slothPlayer) {
    int id = ping.getId();
    if (id == (short) id && slothPlayer.didWeSendThatTrans.remove((short) id)) {
      slothPlayer.entitiesDespawnedThisTransaction.clear();
      slothPlayer.transactionsSent.add(new Pair<>((short) id, System.nanoTime()));
      slothPlayer.getLastTransactionSent().getAndIncrement();
    }
  }

  private void handleSpawnEntity(WrapperPlayServerSpawnEntity spawn, SlothPlayer slothPlayer) {
    if (slothPlayer.entitiesDespawnedThisTransaction.contains(spawn.getEntityId())) {
      slothPlayer.sendTransaction();
    }
    slothPlayer
        .getLatencyUtils()
        .addRealTimeTask(
            slothPlayer.getLastTransactionSent().get(),
            () ->
                slothPlayer
                    .getCompensatedEntities()
                    .addEntity(
                        spawn.getEntityId(), spawn.getUUID().orElse(null), spawn.getEntityType()));
  }

  private void handleSpawnLivingEntity(
      WrapperPlayServerSpawnLivingEntity spawn, SlothPlayer slothPlayer) {
    if (slothPlayer.entitiesDespawnedThisTransaction.contains(spawn.getEntityId())) {
      slothPlayer.sendTransaction();
    }
    slothPlayer
        .getLatencyUtils()
        .addRealTimeTask(
            slothPlayer.getLastTransactionSent().get(),
            () ->
                slothPlayer
                    .getCompensatedEntities()
                    .addEntity(spawn.getEntityId(), spawn.getEntityUUID(), spawn.getEntityType()));
  }

  private void handleSpawnPainting(WrapperPlayServerSpawnPainting spawn, SlothPlayer slothPlayer) {
    if (slothPlayer.entitiesDespawnedThisTransaction.contains(spawn.getEntityId())) {
      slothPlayer.sendTransaction();
    }
    slothPlayer
        .getLatencyUtils()
        .addRealTimeTask(
            slothPlayer.getLastTransactionSent().get(),
            () ->
                slothPlayer
                    .getCompensatedEntities()
                    .addEntity(spawn.getEntityId(), spawn.getUUID(), EntityTypes.PAINTING));
  }

  private void handleSpawnPlayer(WrapperPlayServerSpawnPlayer spawn, SlothPlayer slothPlayer) {
    if (slothPlayer.entitiesDespawnedThisTransaction.contains(spawn.getEntityId())) {
      slothPlayer.sendTransaction();
    }
    slothPlayer
        .getLatencyUtils()
        .addRealTimeTask(
            slothPlayer.getLastTransactionSent().get(),
            () ->
                slothPlayer
                    .getCompensatedEntities()
                    .addEntity(spawn.getEntityId(), spawn.getUUID(), EntityTypes.PLAYER));
  }

  private void handleDestroyEntities(
      WrapperPlayServerDestroyEntities destroy, SlothPlayer slothPlayer) {
    for (int id : destroy.getEntityIds()) {
      slothPlayer.entitiesDespawnedThisTransaction.add(id);
    }
    slothPlayer
        .getLatencyUtils()
        .addRealTimeTask(
            slothPlayer.getLastTransactionSent().get() + 1,
            () -> {
              for (int id : destroy.getEntityIds()) {
                slothPlayer.getCompensatedEntities().removeEntity(id);
              }
            });
  }

  private void handleJoinGame(WrapperPlayServerJoinGame join, SlothPlayer slothPlayer) {
    slothPlayer
        .getLatencyUtils()
        .addRealTimeTask(
            slothPlayer.getLastTransactionSent().get(),
            () -> {
              slothPlayer.setEntityId(join.getEntityId());
              slothPlayer.setGameMode(join.getGameMode());
              slothPlayer.getCompensatedEntities().clear();
            });
  }

  private void handleRespawn(SlothPlayer slothPlayer) {
    slothPlayer
        .getLatencyUtils()
        .addRealTimeTask(
            slothPlayer.getLastTransactionSent().get(),
            () -> slothPlayer.getCompensatedEntities().clear());
  }

  private void handlePositionAndLook(
      WrapperPlayServerPlayerPositionAndLook wrapper, SlothPlayer slothPlayer) {
    slothPlayer.sendTransaction();
    int transactionId = slothPlayer.getLastTransactionSent().get();
    Vector3d location = new Vector3d(wrapper.getX(), wrapper.getY(), wrapper.getZ());
    RelativeFlag flags = wrapper.getRelativeFlags();
    slothPlayer
        .getPendingTeleports()
        .add(new SlothPlayer.TeleportData(location, flags, transactionId));
  }

  private void handlePlayerRotation(
      WrapperPlayServerPlayerRotation wrapper, SlothPlayer slothPlayer) {
    slothPlayer.sendTransaction();
    int transactionId = slothPlayer.getLastTransactionSent().get();
    slothPlayer
        .getPendingRotations()
        .add(new SlothPlayer.RotationData(wrapper.getYaw(), wrapper.getPitch(), transactionId));
  }

  private boolean addTransactionResponse(SlothPlayer player, short id) {
    Pair<Short, Long> data = null;
    boolean hasID = false;

    for (Pair<Short, Long> iterator : player.transactionsSent) {
      if (iterator.first() == id) {
        hasID = true;
        break;
      }
    }

    if (hasID) {
      do {
        data = player.transactionsSent.poll();
        if (data == null) break;
        player.getLastTransactionReceived().incrementAndGet();
      } while (data.first() != id);

      player
          .getLatencyUtils()
          .handleNettySyncTransaction(player.getLastTransactionReceived().get());
    }
    return data != null;
  }

  private void isMojangStupid(
      SlothPlayer player, WrapperPlayClientPlayerFlying flying, PacketReceiveEvent event) {
    if (player.packetStateData.lastPacketWasTeleport) return;
    if (player.getUser().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21)) return;

    final Location location = flying.getLocation();
    final double threshold = player.getMovementThreshold();
    final boolean inVehicle = player.getCompensatedEntities().self.getRiding() != null;

    if (!player.packetStateData.lastPacketWasTeleport
        && flying.hasPositionChanged()
        && flying.hasRotationChanged()
        && ((flying.isOnGround() == player.packetStateData.packetPlayerOnGround
                && (player.getUser().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)
                    && player.packetStateData.lastClaimedPosition.distanceSquared(
                            location.getPosition())
                        < threshold * threshold))
            || inVehicle)) {

      if (player.isCancelDuplicatePacket()) {
        event.setCancelled(true);
      }

      player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = true;

      if (!player.packetStateData.ignoreDuplicatePacketRotation) {
        if (player.yaw != location.getYaw() || player.pitch != location.getPitch()) {
          player.lastYaw = player.yaw;
          player.lastPitch = player.pitch;
        }
        player.yaw = location.getYaw();
        player.pitch = location.getPitch();
      }

      player.packetStateData.lastClaimedPosition = location.getPosition();
    }
  }
}
