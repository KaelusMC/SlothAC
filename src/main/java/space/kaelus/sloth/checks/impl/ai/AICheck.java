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
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.checks.Check;
import space.kaelus.sloth.checks.CheckData;
import space.kaelus.sloth.checks.type.PacketCheck;
import space.kaelus.sloth.config.ConfigManager;
import space.kaelus.sloth.data.TickData;
import space.kaelus.sloth.flatbuffers.TickDataSequence;
import space.kaelus.sloth.player.SlothPlayer;
import space.kaelus.sloth.server.AIServer;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@CheckData(name = "AI (Aim)")
public class AICheck extends Check implements PacketCheck {
    private int sequence;
    private int step;
    private boolean debug;
    private final Deque<TickData> ticks;
    private int ticksStep = 0;

    @Getter private volatile double buffer = 0.0;
    @Getter private volatile double lastProbability = 0.0;

    @Getter @Setter private int prob90 = 0;
    private boolean aiDamageReductionEnabled;
    private double aiDamageReductionProb;
    private double aiDamageReductionMultiplier;

    private double flag;
    private double bufferResetOnFlag;
    private double bufferMultiplier;
    private double bufferDecrease;

    private static final double CHEAT_PROBABILITY = 0.90;
    private static final double LEGIT_PROBABILITY = 0.10;
    private static final Gson GSON = new Gson();

    public AICheck(SlothPlayer slothPlayer) {
        super(slothPlayer);
        reload();
        this.ticks = new ArrayDeque<>(this.sequence);
    }

    public void reload() {
        SlothAC plugin = SlothAC.getInstance();
        ConfigManager configManager = plugin.getConfigManager();

        this.sequence = configManager.getAiSequence();
        this.step = configManager.getAiStep();
        this.debug = configManager.isAiDebug();
        this.aiDamageReductionEnabled = configManager.isAiDamageReductionEnabled();
        this.aiDamageReductionProb = configManager.getAiDamageReductionProb();
        this.aiDamageReductionMultiplier = configManager.getAiDamageReductionMultiplier();

        this.flag = configManager.getAiFlag();
        this.bufferResetOnFlag = configManager.getAiResetOnFlag();
        this.bufferMultiplier = configManager.getAiBufferMultiplier();
        this.bufferDecrease = configManager.getAiBufferDecrease();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (SlothAC.getInstance().getAiServer() == null) return;
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        if (slothPlayer.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
            if (debug) {
                SlothAC.getInstance().getLogger().warning("Mojang failed IQ Test for: " + slothPlayer.getPlayer().getName() + ".");
                return;
            }
        }

        if (slothPlayer.packetStateData.lastPacketWasServerRotation) {
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
            if (SlothAC.getInstance().getConfigManager().isAiWorldGuardEnabled() &&
                    SlothAC.getInstance().getWorldGuardManager().isPlayerInDisabledRegion(slothPlayer.getPlayer())) {
                if (debug) {
                    SlothAC.getInstance().getLogger().info("[AICheck] Player " + slothPlayer.getPlayer().getName() + " is in a disabled region.");
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
        final AIServer client = SlothAC.getInstance().getAiServer();
        if (client == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(SlothAC.getInstance(), () -> {
            try {
                byte[] flatbuffer = serialize(data);
                client.sendRequest(flatbuffer).whenComplete((response, error) -> {
                    if (error != null) {
                        onError(error);
                    } else {
                        onResponse(response);
                    }
                });
            } catch (Exception e) {
                SlothAC.getInstance().getLogger().warning("[AICheck] Failed to send data for " + slothPlayer.getPlayer().getName() + ": " + e.getMessage());
            }
        });
    }

    private void onResponse(String response) {
        try {
            JsonObject jsonObject = GSON.fromJson(response, JsonObject.class);
            if (!jsonObject.has("probability")) {
                SlothAC.getInstance().getLogger().warning("[AICheck] API response is missing probability. Response: " + response);
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

            if (debug) {
                SlothAC.getInstance().getLogger().info(String.format("[AICheck DEBUG | %s] Prob: %.4f | Buffer: %.2f -> %.2f | Damage Multiplier: %.2f",
                        this.slothPlayer.getPlayer().getName(), probability, oldBuffer, this.buffer, slothPlayer.getDmgMultiplier()));
            }

            if (this.buffer > this.flag) {
                flag("prob=" + String.format("%.2f", probability) + " buffer=" + String.format("%.1f", this.buffer));
                this.buffer = this.bufferResetOnFlag;
            }

        } catch (JsonSyntaxException e) {
            SlothAC.getInstance().getLogger().warning("[AICheck] Error parsing API response: " + e.getMessage() + ". Response Body: " + response);
            this.lastProbability = 0.0;
            slothPlayer.setDmgMultiplier(1.0);
        } catch (Exception e) {
            SlothAC.getInstance().getLogger().warning("[AICheck] Unexpected error processing API response: " + e.getMessage());
            e.printStackTrace();
            this.lastProbability = 0.0;
            slothPlayer.setDmgMultiplier(1.0);
        }
    }

    private void onError(Throwable error) {
        this.lastProbability = 0.0;
        slothPlayer.setDmgMultiplier(1.0);
        Throwable cause = (error instanceof java.util.concurrent.CompletionException && error.getCause() != null) ? error.getCause() : error;
        if (cause instanceof AIServer.RequestException e) {
            if (e.getCode() == AIServer.ResponseCode.WAITING) {
                return;
            }
            if (e.getCode() == AIServer.ResponseCode.INVALID_SEQUENCE) {
                try {
                    JsonObject sequenceError = GSON.fromJson(e.getMessage().split(": ", 2)[1], JsonObject.class);
                    if (sequenceError.has("details") && sequenceError.get("details").isJsonObject()) {
                        JsonObject details = sequenceError.getAsJsonObject("details");
                        if (details.has("sequence")) {
                            int newSequence = details.get("sequence").getAsInt();
                            SlothAC.getInstance().getLogger().info("[AICheck] Received new sequence length " + newSequence);
                            this.sequence = newSequence;
                            this.ticks.clear();
                            return;
                        }
                    }
                } catch (Exception parseEx) {
                    SlothAC.getInstance().getLogger().warning("[AICheck] Failed to parse correct sequence: " + parseEx.getMessage());
                }
            }
            SlothAC.getInstance().getLogger().warning("[AICheck] API Error " + e.getCode() + " for player " + slothPlayer.getPlayer().getName() + ": " + e.getMessage());
        } else {
            SlothAC.getInstance().getLogger().warning("[AICheck] Unknown API Error for " + slothPlayer.getPlayer().getName() + ": " + cause.getMessage());
        }
    }

    private byte[] serialize(List<TickData> ticks) {
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);
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