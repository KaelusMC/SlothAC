package space.kaelus.sloth.checks.impl.ai;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.Getter;
import lombok.Setter;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.data.DataSession;

public class DataCollectorManager {
  private final SlothAC plugin;
  @Getter private final Map<UUID, DataSession> activeSessions = new ConcurrentHashMap<>();
  @Setter @Getter private String globalCollectionId = null;

  public DataCollectorManager(SlothAC plugin) {
    this.plugin = plugin;
  }

  public boolean startCollecting(UUID uuid, String playerName, String status) {
    if (activeSessions.containsKey(uuid)) {
      if (activeSessions.get(uuid).getStatus().equals(status)) {
        return false;
      }
      stopCollecting(uuid);
    }
    activeSessions.put(uuid, new DataSession(uuid, playerName, status));
    return true;
  }

  public boolean stopCollecting(UUID uuid) {
    DataSession session = activeSessions.remove(uuid);
    if (session != null) {
      plugin
          .getServer()
          .getScheduler()
          .runTaskAsynchronously(
              plugin,
              () -> {
                try {
                  session.saveAndClose(plugin);
                } catch (IOException e) {
                  plugin.getLogger().log(Level.SEVERE, "Failed to save data for " + uuid, e);
                }
              });
      return true;
    }
    return false;
  }

  public int stopGlobalCollection() {
    final String currentGlobalId = getGlobalCollectionId();
    if (currentGlobalId == null) {
      return 0;
    }

    final List<DataSession> sessionsToArchive = new ArrayList<>();
    for (DataSession session : activeSessions.values()) {
      if (currentGlobalId.equals(session.getStatus())) {
        sessionsToArchive.add(session);
      }
    }

    if (sessionsToArchive.isEmpty()) {
      setGlobalCollectionId(null);
      return 0;
    }

    for (final DataSession session : sessionsToArchive) {
      activeSessions.remove(session.getUuid());
    }
    setGlobalCollectionId(null);

    plugin
        .getServer()
        .getScheduler()
        .runTaskAsynchronously(
            plugin, () -> archiveAndSaveSessions(sessionsToArchive, currentGlobalId));

    return sessionsToArchive.size();
  }

  private void archiveAndSaveSessions(List<DataSession> sessions, String archiveName) {
    File dataFolder = new File(plugin.getDataFolder(), "datacollection");
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
      plugin.getLogger().severe("Failed to create data collection archive: " + e.getMessage());
    }
  }

  public DataSession getSession(UUID uuid) {
    return activeSessions.get(uuid);
  }
}
