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
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import org.bukkit.entity.Player;
import space.kaelus.sloth.player.PlayerDataManager;
import space.kaelus.sloth.player.SlothPlayer;
import space.kaelus.sloth.utils.data.HeadRotation;
import space.kaelus.sloth.utils.data.Pair;
import space.kaelus.sloth.utils.update.RotationUpdate;

public class PacketListener extends PacketListenerAbstract {
    private final PlayerDataManager playerDataManager;

    public PacketListener(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = event.getPlayer();
        SlothPlayer slothPlayer = playerDataManager.getPlayer(player);
        if (slothPlayer == null) return;

        if (event.getPacketType() == PacketType.Play.Client.WINDOW_CONFIRMATION) {
            WrapperPlayClientWindowConfirmation transaction = new WrapperPlayClientWindowConfirmation(event);
            if (transaction.getActionId() <= 0 && addTransactionResponse(slothPlayer, transaction.getActionId())) {
                event.setCancelled(true);
            }
            return;
        }
        if (event.getPacketType() == PacketType.Play.Client.PONG) {
            WrapperPlayClientPong pong = new WrapperPlayClientPong(event);
            int id = pong.getId();
            if (id == (short) id && addTransactionResponse(slothPlayer, (short) id)) {
                event.setCancelled(true);
            }
            return;
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
            slothPlayer.packetStateData.lastPacketWasOnePointSeventeenDuplicate = isMojangStupid(slothPlayer, flying);

            if (slothPlayer.packetStateData.cancelDuplicatePacket && slothPlayer.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
                event.setCancelled(true);
            }
        }

        if (event.isCancelled()) {
            slothPlayer.packetStateData.lastPacketWasOnePointSeventeenDuplicate = false;
            slothPlayer.packetStateData.lastPacketWasServerRotation = false;
            return;
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            WrapperPlayClientPlayerFlying packet = new WrapperPlayClientPlayerFlying(event);
            boolean ignoreRotation = slothPlayer.packetStateData.lastPacketWasOnePointSeventeenDuplicate && slothPlayer.packetStateData.ignoreDuplicatePacketRotation;

            if (packet.hasPositionChanged()) {
                slothPlayer.packetStateData.lastClaimedPosition = packet.getLocation().getPosition();
            }

            if (packet.hasRotationChanged() && !ignoreRotation) {
                float newYaw = packet.getLocation().getYaw();
                float newPitch = packet.getLocation().getPitch();
                float deltaYaw = newYaw - slothPlayer.yaw;
                float deltaPitch = newPitch - slothPlayer.pitch;

                RotationUpdate update = new RotationUpdate(new HeadRotation(slothPlayer.yaw, slothPlayer.pitch), new HeadRotation(newYaw, newPitch), deltaYaw, deltaPitch);
                slothPlayer.getCheckManager().onRotationUpdate(update);

                slothPlayer.lastYaw = slothPlayer.yaw;
                slothPlayer.lastPitch = slothPlayer.pitch;
                slothPlayer.yaw = newYaw;
                slothPlayer.pitch = newPitch;
            }
        }

        slothPlayer.getCheckManager().onPacketReceive(event);

        slothPlayer.packetStateData.lastPacketWasOnePointSeventeenDuplicate = false;
        slothPlayer.packetStateData.lastPacketWasServerRotation = false;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = event.getPlayer();
        SlothPlayer slothPlayer = playerDataManager.getPlayer(player);
        if (slothPlayer == null) return;

        if (event.getPacketType() == PacketType.Play.Server.WINDOW_CONFIRMATION) {
            WrapperPlayServerWindowConfirmation confirmation = new WrapperPlayServerWindowConfirmation(event);
            short id = confirmation.getActionId();
            if (id <= 0 && slothPlayer.didWeSendThatTrans.remove(id)) {
                slothPlayer.transactionsSent.add(new Pair<>(id, System.nanoTime()));
                slothPlayer.getLastTransactionSent().getAndIncrement();
            }
        }
        if (event.getPacketType() == PacketType.Play.Server.PING) {
            WrapperPlayServerPing ping = new WrapperPlayServerPing(event);
            int id = ping.getId();
            if (id == (short)id && slothPlayer.didWeSendThatTrans.remove((short)id)) {
                slothPlayer.transactionsSent.add(new Pair<>((short)id, System.nanoTime()));
                slothPlayer.getLastTransactionSent().getAndIncrement();
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
            WrapperPlayServerChunkData chunkData = new WrapperPlayServerChunkData(event);
            slothPlayer.getCompensatedWorld().addToCache(new space.kaelus.sloth.world.Column(chunkData.getColumn().getX(), chunkData.getColumn().getZ(), chunkData.getColumn().getChunks(), slothPlayer.getLastTransactionSent().get()), chunkData.getColumn().getX(), chunkData.getColumn().getZ());
        }

        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(event);
            slothPlayer.getLatencyUtils().addRealTimeTask(slothPlayer.getLastTransactionSent().get(), () ->
                    slothPlayer.getCompensatedEntities().addEntity(spawn.getEntityId(), spawn.getUUID().orElse(null), spawn.getEntityType()));
        }
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_PLAYER) {
            WrapperPlayServerSpawnPlayer spawn = new WrapperPlayServerSpawnPlayer(event);
            slothPlayer.getLatencyUtils().addRealTimeTask(slothPlayer.getLastTransactionSent().get(), () ->
                    slothPlayer.getCompensatedEntities().addEntity(spawn.getEntityId(), spawn.getUUID(), EntityTypes.PLAYER));
        }
        if (event.getPacketType() == PacketType.Play.Server.DESTROY_ENTITIES) {
            WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(event);
            slothPlayer.getLatencyUtils().addRealTimeTask(slothPlayer.getLastTransactionSent().get() + 1, () -> {
                for(int id : destroy.getEntityIds()) {
                    slothPlayer.getCompensatedEntities().removeEntity(id);
                }
            });
        }

        if (event.getPacketType() == PacketType.Play.Server.JOIN_GAME) {
            WrapperPlayServerJoinGame join = new WrapperPlayServerJoinGame(event);
            slothPlayer.getLatencyUtils().addRealTimeTask(slothPlayer.getLastTransactionSent().get(), () -> {
                slothPlayer.setEntityId(join.getEntityId());
                slothPlayer.setGameMode(join.getGameMode());
                slothPlayer.getCompensatedEntities().clear();
                slothPlayer.getCompensatedWorld().clear();
            });
        }
        if (event.getPacketType() == PacketType.Play.Server.RESPAWN) {
            slothPlayer.getLatencyUtils().addRealTimeTask(slothPlayer.getLastTransactionSent().get(), () -> {
                slothPlayer.getCompensatedEntities().clear();
                slothPlayer.getCompensatedWorld().clear();
            });
        }

        if (event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
            WrapperPlayServerPlayerPositionAndLook wrapper = new WrapperPlayServerPlayerPositionAndLook(event);
            if(slothPlayer.yaw != wrapper.getYaw() || slothPlayer.pitch != wrapper.getPitch()) {
                slothPlayer.packetStateData.lastPacketWasServerRotation = true;
            }
            slothPlayer.sendTransaction();
        }
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

            player.getLatencyUtils().handleNettySyncTransaction(player.getLastTransactionReceived().get());
        }
        return data != null;
    }

    private boolean isMojangStupid(SlothPlayer player, WrapperPlayClientPlayerFlying flying) {
        if (player.packetStateData.lastPacketWasServerRotation) return false;
        if (player.getUser().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21)) return false;

        final Location location = flying.getLocation();

        return !player.packetStateData.lastPacketWasServerRotation && flying.hasPositionChanged() && flying.hasRotationChanged() &&
                ((flying.isOnGround() == player.getPlayer().isOnGround()
                        && (player.getUser().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17) &&
                        Math.abs(player.packetStateData.lastClaimedPosition.getX() - location.getX()) < 0.0001 &&
                        Math.abs(player.packetStateData.lastClaimedPosition.getY() - location.getY()) < 0.0001 &&
                        Math.abs(player.packetStateData.lastClaimedPosition.getZ() - location.getZ()) < 0.0001
                )));
    }
}