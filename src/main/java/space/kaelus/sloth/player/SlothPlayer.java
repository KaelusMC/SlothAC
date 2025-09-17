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
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.util.Vector3d;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.alert.AlertManager;
import space.kaelus.sloth.checks.CheckManager;
import space.kaelus.sloth.checks.impl.ai.DataCollectorManager;
import space.kaelus.sloth.config.ConfigManager;
import space.kaelus.sloth.database.DatabaseManager;
import space.kaelus.sloth.entity.CompensatedEntities;
import space.kaelus.sloth.integration.WorldGuardManager;
import space.kaelus.sloth.punishment.PunishmentManager;
import space.kaelus.sloth.server.AIServerProvider;
import space.kaelus.sloth.utils.data.HeadRotation;
import space.kaelus.sloth.utils.data.PacketStateData;
import space.kaelus.sloth.utils.data.Pair;
import space.kaelus.sloth.utils.latency.ILatencyUtils;
import space.kaelus.sloth.utils.latency.LatencyUtils;
import space.kaelus.sloth.utils.update.RotationUpdate;
import space.kaelus.sloth.world.CompensatedWorld;

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
  public final RotationUpdate rotationUpdate =
      new RotationUpdate(new HeadRotation(), new HeadRotation(), 0, 0);

  public final long joinTime;
  @Setter private int entityId;
  @Setter private GameMode gameMode = GameMode.SURVIVAL;
  @Setter private String brand = "vanilla";

  public double x, y, z;
  public float yaw, pitch;
  public float lastYaw, lastPitch;

  private final Queue<TeleportData> pendingTeleports = new ConcurrentLinkedQueue<>();
  private final Queue<RotationData> pendingRotations = new ConcurrentLinkedQueue<>();

  @Setter private double dmgMultiplier = 1.0;
  public int ticksSinceAttack;

  public final Queue<Pair<Short, Long>> transactionsSent = new ConcurrentLinkedQueue<>();
  public final IntArraySet entitiesDespawnedThisTransaction = new IntArraySet();
  public final Set<Short> didWeSendThatTrans = ConcurrentHashMap.newKeySet();
  public final AtomicInteger lastTransactionSent = new AtomicInteger(0);
  public final AtomicInteger lastTransactionReceived = new AtomicInteger(0);
  private final AtomicInteger transactionIDCounter = new AtomicInteger(0);
  private final SlothAC plugin;

  public SlothPlayer(
      Player player,
      SlothAC plugin,
      ConfigManager configManager,
      DatabaseManager databaseManager,
      AlertManager alertManager,
      DataCollectorManager dataCollectorManager,
      AIServerProvider aiServerProvider,
      WorldGuardManager worldGuardManager) {
    this.plugin = plugin;
    this.player = player;
    this.uuid = player.getUniqueId();
    this.user = PacketEvents.getAPI().getPlayerManager().getUser(player);
    this.joinTime = System.currentTimeMillis();

    this.latencyUtils = new LatencyUtils(this, plugin);
    this.compensatedWorld = new CompensatedWorld(this);
    this.compensatedEntities = new CompensatedEntities(this);

    this.checkManager =
        new CheckManager(
            this,
            plugin,
            configManager,
            dataCollectorManager,
            aiServerProvider,
            worldGuardManager,
            alertManager);

    this.punishmentManager =
        new PunishmentManager(
            this, plugin, configManager, databaseManager.getDatabase(), alertManager);

    int sequence = configManager.getAiSequence();
    this.ticksSinceAttack = sequence + 1;
  }

  public boolean isPointThree() {
    return getUser().getClientVersion().isOlderThan(ClientVersion.V_1_18_2);
  }

  public double getMovementThreshold() {
    return isPointThree() ? 0.03 : 0.0002;
  }

  public boolean isCancelDuplicatePacket() {
    return true;
  }

  public void sendTransaction() {
    if (user.getConnectionState()
        != com.github.retrooper.packetevents.protocol.ConnectionState.PLAY) return;

    short transactionID = (short) (-1 * (transactionIDCounter.getAndIncrement() & 0x7FFF));
    didWeSendThatTrans.add(transactionID);

    com.github.retrooper.packetevents.wrapper.PacketWrapper<?> packet;
    if (PacketEvents.getAPI()
        .getServerManager()
        .getVersion()
        .isNewerThanOrEquals(
            com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_17)) {
      packet =
          new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing(
              transactionID);
    } else {
      packet =
          new com.github.retrooper.packetevents.wrapper.play.server
              .WrapperPlayServerWindowConfirmation((byte) 0, transactionID, false);
    }
    user.sendPacket(packet);
  }

  public void disconnect(Component reason) {
    String textReason =
        net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
            .serialize(reason);
    user.sendPacket(
        new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect(
            reason));
    user.closeConnection();

    if (Bukkit.isPrimaryThread()) {
      player.kickPlayer(textReason);
    } else {
      Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer(textReason));
    }
  }

  public void reload() {
    if (this.punishmentManager != null) {
      this.punishmentManager.reload();
    }
    if (this.checkManager != null) {
      this.checkManager.reloadChecks();
    }
  }

  @RequiredArgsConstructor
  @Getter
  public static class TeleportData {
    private final Vector3d location;
    private final RelativeFlag flags;
    private final int transactionId;

    public boolean isRelativeX() {
      return flags.has(RelativeFlag.X);
    }

    public boolean isRelativeY() {
      return flags.has(RelativeFlag.Y);
    }

    public boolean isRelativeZ() {
      return flags.has(RelativeFlag.Z);
    }
  }

  @RequiredArgsConstructor
  @Getter
  public static class RotationData {
    private final float yaw;
    private final float pitch;
    private final int transactionId;
  }
}
