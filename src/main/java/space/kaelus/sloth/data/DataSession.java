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
package space.kaelus.sloth.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import space.kaelus.sloth.SlothAC;

@Getter
public class DataSession {
  private static final DateTimeFormatter TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());
  private final UUID uuid;
  private final String player;
  private final String status;
  private final java.util.Queue<TickData> recordedTicks =
      new java.util.concurrent.ConcurrentLinkedQueue<>();
  private final Instant startTime;

  public DataSession(UUID uuid, String player, String status) {
    this.uuid = uuid;
    this.player = player;
    this.status = status;
    this.startTime = Instant.now();
  }

  public void addTick(TickData tickData) {
    recordedTicks.add(tickData);
  }

  public String generateFileName() {
    String timestamp = TIMESTAMP_FORMAT.format(startTime);
    String statusForFilename = status.replace(' ', '#').replaceAll("[/\\?%*:|\"<>']", "-");
    int globalIndex = statusForFilename.indexOf("_GLOBAL_");
    if (globalIndex >= 0) {
      statusForFilename = statusForFilename.substring(0, globalIndex);
    }
    return String.format("%s_%s_%s.csv", statusForFilename, player, timestamp);
  }

  public String generateCsvContent() {
    if (recordedTicks.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append(TickData.getHeader()).append("\n");
    String cheatingStatus = "UNLABELED";
    if (status.startsWith("CHEAT")) {
      cheatingStatus = "CHEAT";
    } else if (status.startsWith("LEGIT")) {
      cheatingStatus = "LEGIT";
    }
    List<TickData> ticks = new ArrayList<>(recordedTicks);
    for (TickData tick : ticks) {
      sb.append(tick.toCsv(cheatingStatus)).append("\n");
    }
    return sb.toString();
  }

  public void saveAndClose(SlothAC plugin) throws IOException {
    String csvContent = generateCsvContent();
    if (csvContent.isEmpty()) {
      return;
    }
    File dataFolder = new File(plugin.getDataFolder(), "datacollection");
    if (!dataFolder.exists()) {
      dataFolder.mkdirs();
    }
    File outputFile = new File(dataFolder, generateFileName());
    try (BufferedWriter writer =
        Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)) {
      writer.write(csvContent);
    }
  }
}
