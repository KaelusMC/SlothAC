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
import lombok.Getter;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.checks.Check;
import space.kaelus.sloth.checks.CheckData;
import space.kaelus.sloth.checks.type.PacketCheck;
import space.kaelus.sloth.data.DataSession;
import space.kaelus.sloth.data.TickData;
import space.kaelus.sloth.player.SlothPlayer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@CheckData(name = "DataCollector")
public class DataCollectorCheck extends Check implements PacketCheck {
    private static final Map<UUID, DataSession> activeSessions = new ConcurrentHashMap<>();
    @Getter
    private static String globalCollectionId = null;

    public DataCollectorCheck(SlothPlayer slothPlayer) {
        super(slothPlayer);
    }

    public static synchronized void setGlobalCollectionId(String id) {
        globalCollectionId = id;
    }

    public static synchronized Map<UUID, DataSession> getActiveSessions() {
        return activeSessions;
    }

    public static synchronized boolean startCollecting(UUID uuid, String playerName, String status) {
        if (activeSessions.containsKey(uuid)) {
            if (activeSessions.get(uuid).getStatus().equals(status)) {
                return false;
            }
            stopCollecting(uuid);
        }
        activeSessions.put(uuid, new DataSession(uuid, playerName, status));
        return true;
    }

    public static synchronized boolean stopCollecting(UUID uuid) {
        DataSession session = activeSessions.remove(uuid);
        if (session != null) {
            try {
                session.saveAndClose();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    public static synchronized int stopGlobalCollection() {
        String currentGlobalId = getGlobalCollectionId();
        if (currentGlobalId == null) {
            return 0;
        }
        List<DataSession> sessionsToArchive = new ArrayList<>();
        for (DataSession session : activeSessions.values()) {
            if (currentGlobalId.equals(session.getStatus())) {
                sessionsToArchive.add(session);
            }
        }
        if (!sessionsToArchive.isEmpty()) {
            archiveAndSaveSessions(sessionsToArchive, currentGlobalId);
            for (DataSession session : sessionsToArchive) {
                activeSessions.remove(session.getUuid());
            }
        }
        setGlobalCollectionId(null);
        return sessionsToArchive.size();
    }

    private static void archiveAndSaveSessions(List<DataSession> sessions, String archiveName) {
        File dataFolder = new File(SlothAC.getInstance().getDataFolder(), "datacollection");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File zipFile = new File(dataFolder, archiveName + ".zip");
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (DataSession session : sessions) {
                if (session.getRecordedTicks().isEmpty()) continue;
                String fileName = session.generateFileName();
                String csvContent = session.generateCsvContent();
                ZipEntry zipEntry = new ZipEntry(fileName);
                zos.putNextEntry(zipEntry);
                zos.write(csvContent.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        } catch (IOException e) {
            SlothAC.getInstance().getLogger().severe("Failed to create data collection archive: " + e.getMessage());
        }
    }

    public static DataSession getSession(UUID uuid) {
        return activeSessions.get(uuid);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (slothPlayer == null) return;
        DataSession session = activeSessions.get(slothPlayer.getUuid());
        if (session == null) return;
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            if (slothPlayer.packetStateData.lastPacketWasServerRotation) {
                SlothAC.getInstance().getLogger().info("Skipping server-side rotation packet in data collection for player: " + slothPlayer.getPlayer().getName());
                return;
            }
            if (slothPlayer.getTicksSinceAttack() < 40) {
                session.addTick(new TickData(slothPlayer));
            }
        }
    }
}