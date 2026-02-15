/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2026 KaelusMC
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package space.kaelus.sloth.data

import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Queue
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import space.kaelus.sloth.SlothAC

class DataSession(val uuid: UUID, val player: String, val status: String) {
  val recordedTicks: Queue<TickData> = ConcurrentLinkedQueue()
  val startTime: Instant = Instant.now()

  fun addTick(tickData: TickData) {
    recordedTicks.add(tickData)
  }

  fun generateFileName(): String {
    val timestamp = TIMESTAMP_FORMAT.format(startTime)
    var statusForFilename = status.replace(' ', '#').replace(Regex("[/\\\\?%*:|\"<>']"), "-")
    val globalIndex = statusForFilename.indexOf("_GLOBAL_")
    if (globalIndex >= 0) {
      statusForFilename = statusForFilename.substring(0, globalIndex)
    }
    return String.format("%s_%s_%s.csv", statusForFilename, player, timestamp)
  }

  fun writeCsv(writer: Appendable) {
    if (recordedTicks.isEmpty()) {
      return
    }
    writer.append(TickData.getHeader()).append('\n')
    val cheatingStatus =
      when {
        status.startsWith("CHEAT") -> "CHEAT"
        status.startsWith("LEGIT") -> "LEGIT"
        else -> "UNLABELED"
      }
    for (tick in recordedTicks) {
      tick.appendCsv(writer, cheatingStatus)
      writer.append('\n')
    }
  }

  @Throws(IOException::class)
  fun saveAndClose(plugin: SlothAC) {
    if (recordedTicks.isEmpty()) {
      return
    }
    val dataFolder = File(plugin.dataFolder, "datacollection")
    if (!dataFolder.exists()) {
      dataFolder.mkdirs()
    }
    val outputFile = File(dataFolder, generateFileName())
    Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8).use { writer ->
      writeCsv(writer)
    }
  }

  private companion object {
    private val TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault())
  }
}
