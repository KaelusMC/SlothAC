/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2025 KaelusMC
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
package space.kaelus.sloth.checks.impl.ai;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.google.flatbuffers.FlatBufferBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.alert.AlertManager;
import space.kaelus.sloth.checks.Check;
import space.kaelus.sloth.checks.CheckData;
import space.kaelus.sloth.checks.Reloadable;
import space.kaelus.sloth.checks.type.PacketCheck;
import space.kaelus.sloth.config.ConfigManager;
import space.kaelus.sloth.data.TickData;
import space.kaelus.sloth.flatbuffers.TickDataSequence;
import space.kaelus.sloth.integration.WorldGuardManager;
import space.kaelus.sloth.player.SlothPlayer;
import space.kaelus.sloth.server.AIServer;
import space.kaelus.sloth.server.AIServerProvider;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;

@CheckData(name = "AI (Aim)")
public class AICheck extends Check implements PacketCheck, Reloadable {
  private final SlothAC plugin;
  private final AIServerProvider aiServerProvider;
  private final ConfigManager configManager;
  private final WorldGuardManager worldGuardManager;
  private final AlertManager alertManager;

  private int step;
  private boolean debug;
  private AIServer aiServer;
  private Deque<TickData> ticks;
  private int ticksStep = 0;

  @Getter private double buffer = 0.0;
  @Getter private double lastProbability = 0.0;

  @Getter @Setter private int prob90 = 0;
  private boolean aiDamageReductionEnabled;
  private double aiDamageReductionProb;
  private double aiDamageReductionMultiplier;

  private double flag;
  private double bufferResetOnFlag;
  private double bufferMultiplier;
  private double bufferDecrease;
  private double suspiciousAlertBuffer;

  private static final double CHEAT_PROBABILITY = 0.90;
  private static final double LEGIT_PROBABILITY = 0.10;
  private static final Gson GSON = new Gson();
  private static final ThreadLocal<FlatBufferBuilder> BUILDER =
      ThreadLocal.withInitial(() -> new FlatBufferBuilder(4096));

  public AICheck(
      SlothPlayer slothPlayer,
      SlothAC plugin,
      AIServerProvider aiServerProvider,
      ConfigManager configManager,
      WorldGuardManager worldGuardManager,
      AlertManager alertManager) {
    super(slothPlayer);
    this.plugin = plugin;
    this.aiServerProvider = aiServerProvider;
    this.configManager = configManager;
    this.worldGuardManager = worldGuardManager;
    this.alertManager = alertManager;
    reload();
  }

  @Override
  public void reload() {
    this.aiServer = this.aiServerProvider.get();

    if (this.ticks == null || this.ticks.size() != configManager.getAiSequence()) {
      this.ticks = new ArrayDeque<>(configManager.getAiSequence());
    }

    this.step = configManager.getAiStep();
    this.debug = configManager.isAiDebug();
    this.aiDamageReductionEnabled = configManager.isAiDamageReductionEnabled();
    this.aiDamageReductionProb = configManager.getAiDamageReductionProb();
    this.aiDamageReductionMultiplier = configManager.getAiDamageReductionMultiplier();

    this.flag = configManager.getAiFlag();
    this.bufferResetOnFlag = configManager.getAiResetOnFlag();
    this.bufferMultiplier = configManager.getAiBufferMultiplier();
    this.bufferDecrease = configManager.getAiBufferDecrease();
    this.suspiciousAlertBuffer = configManager.getSuspiciousAlertsBuffer();
  }

  @Override
  public void onPacketReceive(PacketReceiveEvent event) {
    if (aiServer == null) return;
    if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

    int sequence = configManager.getAiSequence();

    if (slothPlayer.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
      if (debug) {
        plugin
            .getLogger()
            .warning("Mojang failed IQ Test for: " + slothPlayer.getPlayer().getName() + ".");
        return;
      }
    }

    if (slothPlayer.packetStateData.lastPacketWasTeleport
        || slothPlayer.packetStateData.lastPacketWasServerRotation) {
      return;
    }

    if (slothPlayer.getTicksSinceAttack() > sequence) {
      if (!ticks.isEmpty()) {
        ticks.clear();
      }
      ticksStep = 0;
      return;
    }

    ticks.addLast(new TickData(slothPlayer));
    ticksStep++;

    while (ticks.size() > sequence) {
      ticks.removeFirst();
    }

    if (ticks.size() == sequence && ticksStep >= step) {
      if (configManager.isAiWorldGuardEnabled()
          && worldGuardManager.isPlayerInDisabledRegion(slothPlayer.getPlayer())) {
        if (debug) {
          plugin
              .getLogger()
              .info(
                  "[AICheck] Player "
                      + slothPlayer.getPlayer().getName()
                      + " is in a disabled region.");
        }
        ticksStep = 0;
        return;
      }
      sendData();
      ticksStep = 0;
    }
  }

  private void sendData() {
    final List<TickData> data = new ArrayList<>(ticks);
    if (aiServer == null) return;

    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              try {
                byte[] flatbuffer = serialize(data);
                aiServer
                    .sendRequest(flatbuffer)
                    .whenComplete(
                        (response, error) -> {
                          if (error != null) {
                            onError(error);
                          } else {
                            onResponse(response);
                          }
                        })
                    .exceptionally(
                        ex -> {
                          plugin
                              .getLogger()
                              .log(
                                  Level.SEVERE,
                                  "Unhandled exception in AI server response for player "
                                      + slothPlayer.getPlayer().getName(),
                                  ex);
                          return null;
                        });
              } catch (Exception e) {
                plugin
                    .getLogger()
                    .warning(
                        "[AICheck] Failed to send data for "
                            + slothPlayer.getPlayer().getName()
                            + ": "
                            + e.getMessage());
              }
            });
  }

  private void onResponse(String response) {
    try {
      JsonObject jsonObject = GSON.fromJson(response, JsonObject.class);
      if (!jsonObject.has("probability")) {
        plugin
            .getLogger()
            .warning("[AICheck] API response is missing probability. Response: " + response);
        this.lastProbability = 0.0;
        slothPlayer.setDmgMultiplier(1.0);
        return;
      }
      double probability = jsonObject.get("probability").getAsDouble();
      this.lastProbability = probability;

      if (aiDamageReductionEnabled) {
        if (probability >= aiDamageReductionProb) {
          double ratio = (probability - aiDamageReductionProb) / (1.0 - aiDamageReductionProb);
          double reduction = Math.min(1.0, ratio * aiDamageReductionMultiplier);
          slothPlayer.setDmgMultiplier(1.0 - reduction);
        } else {
          slothPlayer.setDmgMultiplier(1.0);
        }
      }

      if (probability > 0.9) {
        prob90++;
      }

      double oldBuffer = this.buffer;

      if (probability > CHEAT_PROBABILITY) {
        this.buffer += (probability - CHEAT_PROBABILITY) * this.bufferMultiplier;
      } else if (probability < LEGIT_PROBABILITY) {
        this.buffer = Math.max(0, this.buffer - this.bufferDecrease);
      }

      if (this.buffer > suspiciousAlertBuffer && oldBuffer <= suspiciousAlertBuffer) {
        alertManager.sendSuspiciousAlert(
            MessageUtil.getMessage(
                Message.SUSPICIOUS_ALERT_TRIGGERED,
                "player",
                this.slothPlayer.getPlayer().getName(),
                "buffer",
                String.format("%.1f", this.buffer)));
      }

      if (debug) {
        plugin
            .getLogger()
            .info(
                String.format(
                    "[AICheck DEBUG | %s] Prob: %.4f | Buffer: %.2f -> %.2f | Damage Multiplier: %.2f",
                    this.slothPlayer.getPlayer().getName(),
                    probability,
                    oldBuffer,
                    this.buffer,
                    slothPlayer.getDmgMultiplier()));
      }

      if (this.buffer > this.flag) {
        flag(
            "prob="
                + String.format("%.2f", probability)
                + " buffer="
                + String.format("%.1f", this.buffer));
        this.buffer = this.bufferResetOnFlag;
      }

    } catch (JsonSyntaxException e) {
      plugin
          .getLogger()
          .warning(
              "[AICheck] Error parsing API response: "
                  + e.getMessage()
                  + ". Response Body: "
                  + response);
      this.lastProbability = 0.0;
      slothPlayer.setDmgMultiplier(1.0);
    } catch (Exception e) {
      plugin
          .getLogger()
          .warning("[AICheck] Unexpected error processing API response: " + e.getMessage());
      e.printStackTrace();
      this.lastProbability = 0.0;
      slothPlayer.setDmgMultiplier(1.0);
    }
  }

  private void onError(Throwable error) {
    this.lastProbability = 0.0;
    slothPlayer.setDmgMultiplier(1.0);
    Throwable cause =
        (error instanceof java.util.concurrent.CompletionException && error.getCause() != null)
            ? error.getCause()
            : error;
    if (cause instanceof AIServer.RequestException e) {
      if (e.getCode() == AIServer.ResponseCode.WAITING) {
        return;
      }
      if (e.getCode() == AIServer.ResponseCode.INVALID_SEQUENCE) {
        try {
          JsonObject json = GSON.fromJson(e.getMessage().split(": ", 2)[1], JsonObject.class);
          if (json.has("details") && json.get("details").isJsonObject()) {
            JsonObject details = json.getAsJsonObject("details");
            if (details.has("sequence")) {
              int newSequence = details.get("sequence").getAsInt();
              if (configManager.getAiSequence() != newSequence) {
                plugin.getLogger().info("[AICheck] Received new sequence length " + newSequence);
                configManager.setAiSequence(newSequence);
                this.ticks = new ArrayDeque<>(newSequence);
              }
              return;
            }
          }
        } catch (Exception parseEx) {
          plugin
              .getLogger()
              .warning("[AICheck] Failed to parse correct sequence: " + parseEx.getMessage());
        }
      }
      plugin
          .getLogger()
          .warning(
              "[AICheck] API Error "
                  + e.getCode()
                  + " for player "
                  + slothPlayer.getPlayer().getName()
                  + ": "
                  + e.getMessage());
    } else {
      plugin
          .getLogger()
          .warning(
              "[AICheck] Unknown API Error for "
                  + slothPlayer.getPlayer().getName()
                  + ": "
                  + cause.getMessage());
    }
  }

  private byte[] serialize(List<TickData> ticks) {
    final FlatBufferBuilder builder = BUILDER.get();

    builder.clear();

    int[] tickOffsets = new int[ticks.size()];

    for (int i = ticks.size() - 1; i >= 0; i--) {
      TickData tick = ticks.get(i);
      space.kaelus.sloth.flatbuffers.TickData.startTickData(builder);
      space.kaelus.sloth.flatbuffers.TickData.addDeltaYaw(builder, tick.deltaYaw);
      space.kaelus.sloth.flatbuffers.TickData.addDeltaPitch(builder, tick.deltaPitch);
      space.kaelus.sloth.flatbuffers.TickData.addAccelYaw(builder, tick.accelYaw);
      space.kaelus.sloth.flatbuffers.TickData.addAccelPitch(builder, tick.accelPitch);
      space.kaelus.sloth.flatbuffers.TickData.addJerkYaw(builder, tick.jerkYaw);
      space.kaelus.sloth.flatbuffers.TickData.addJerkPitch(builder, tick.jerkPitch);
      space.kaelus.sloth.flatbuffers.TickData.addGcdErrorYaw(builder, tick.gcdErrorYaw);
      space.kaelus.sloth.flatbuffers.TickData.addGcdErrorPitch(builder, tick.gcdErrorPitch);
      tickOffsets[i] = space.kaelus.sloth.flatbuffers.TickData.endTickData(builder);
    }

    int ticksVector = TickDataSequence.createTicksVector(builder, tickOffsets);
    TickDataSequence.startTickDataSequence(builder);
    TickDataSequence.addTicks(builder, ticksVector);
    int sequenceOffset = TickDataSequence.endTickDataSequence(builder);
    builder.finish(sequenceOffset);

    ByteBuffer buf = builder.dataBuffer();

    byte[] bytes = new byte[buf.remaining()];
    buf.get(bytes);
    return bytes;
  }
}
