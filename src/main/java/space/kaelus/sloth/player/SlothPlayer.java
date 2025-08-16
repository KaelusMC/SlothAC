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
package space.kaelus.sloth.player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.checks.CheckManager;
import space.kaelus.sloth.checks.impl.ai.AICheck;
import space.kaelus.sloth.entity.CompensatedEntities;
import space.kaelus.sloth.punishment.PunishmentManager;
import space.kaelus.sloth.utils.data.PacketStateData;
import space.kaelus.sloth.utils.data.Pair;
import space.kaelus.sloth.utils.latency.ILatencyUtils;
import space.kaelus.sloth.utils.latency.LatencyUtils;
import space.kaelus.sloth.world.CompensatedWorld;

import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class SlothPlayer {
    private final UUID uuid;
    private final Player player;
    private final User user;
    private final CheckManager checkManager;
    private final PunishmentManager punishmentManager;
    public final CompensatedEntities compensatedEntities;
    public final CompensatedWorld compensatedWorld;
    public final ILatencyUtils latencyUtils;
    public final PacketStateData packetStateData = new PacketStateData();

    public final long joinTime;
    @Setter private int entityId;
    @Setter private GameMode gameMode = GameMode.SURVIVAL;
    @Setter private String brand = "vanilla";

    public float yaw, pitch;
    public float lastYaw, lastPitch;

    @Setter
    private double dmgMultiplier = 1.0;
    public int ticksSinceAttack;

    public final Queue<Pair<Short, Long>> transactionsSent = new ConcurrentLinkedQueue<>();
    public final Set<Short> didWeSendThatTrans = ConcurrentHashMap.newKeySet();
    public final AtomicInteger lastTransactionSent = new AtomicInteger(0);
    public final AtomicInteger lastTransactionReceived = new AtomicInteger(0);
    private final AtomicInteger transactionIDCounter = new AtomicInteger(0);

    public SlothPlayer(Player player) {
        this.player = player;
        this.uuid = player.getUniqueId();
        this.user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        this.joinTime = System.currentTimeMillis();

        this.latencyUtils = new LatencyUtils(this);
        this.compensatedWorld = new CompensatedWorld(this);
        this.compensatedEntities = new CompensatedEntities(this);
        this.checkManager = new CheckManager(this);
        this.punishmentManager = new PunishmentManager(this);

        int sequence = SlothAC.getInstance().getConfigManager().getAiSequence();
        this.ticksSinceAttack = sequence + 1;
    }

    public void sendTransaction() {
        if (user.getConnectionState() != com.github.retrooper.packetevents.protocol.ConnectionState.PLAY) return;

        short transactionID = (short) (-1 * (transactionIDCounter.getAndIncrement() & 0x7FFF));
        didWeSendThatTrans.add(transactionID);

        com.github.retrooper.packetevents.wrapper.PacketWrapper<?> packet;
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_17)) {
            packet = new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing(transactionID);
        } else {
            packet = new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation((byte) 0, transactionID, false);
        }
        user.sendPacket(packet);
    }

    public void disconnect(Component reason) {
        String textReason = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(reason);
        user.sendPacket(new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect(reason));
        user.closeConnection();

        if (Bukkit.isPrimaryThread()) {
            player.kickPlayer(textReason);
        } else {
            Bukkit.getScheduler().runTask(SlothAC.getInstance(), () -> player.kickPlayer(textReason));
        }
    }

    public void reload() {
        if (this.punishmentManager != null) {
            this.punishmentManager.reload();
        }

        if (this.checkManager != null) {
            AICheck aiCheck = this.checkManager.getCheck(AICheck.class);
            if (aiCheck != null) aiCheck.reload();
        }
    }
}