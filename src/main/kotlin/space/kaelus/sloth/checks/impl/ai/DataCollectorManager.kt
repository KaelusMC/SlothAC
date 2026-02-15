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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package space.kaelus.sloth.checks.impl.ai

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.data.DataSession
import space.kaelus.sloth.scheduler.SchedulerService

class DataCollectorManager(private val plugin: SlothAC, private val scheduler: SchedulerService) {
  val activeSessions: MutableMap<UUID, DataSession> = ConcurrentHashMap()
  var globalCollectionId: String? = null

  fun startCollecting(uuid: UUID, playerName: String, status: String): Boolean {
    val existingSession = activeSessions[uuid]
    if (existingSession != null) {
      if (existingSession.status == status) {
        return false
      }
      stopCollecting(uuid)
    }
    activeSessions[uuid] = DataSession(uuid, playerName, status)
    return true
  }

  fun stopCollecting(uuid: UUID): Boolean {
    val session = activeSessions.remove(uuid)
    if (session != null) {
      scheduler.runAsync {
        try {
          session.saveAndClose(plugin)
        } catch (e: IOException) {
          plugin.logger.log(Level.SEVERE, "Failed to save data for $uuid", e)
        }
      }
      return true
    }
    return false
  }

  fun stopGlobalCollection(): Int {
    val currentGlobalId = globalCollectionId ?: return 0

    val sessionsToArchive = ArrayList<DataSession>()
    for (session in activeSessions.values) {
      if (currentGlobalId == session.status) {
        sessionsToArchive.add(session)
      }
    }

    if (sessionsToArchive.isEmpty()) {
      globalCollectionId = null
      return 0
    }

    for (session in sessionsToArchive) {
      activeSessions.remove(session.uuid)
    }
    globalCollectionId = null

    scheduler.runAsync { archiveAndSaveSessions(sessionsToArchive, currentGlobalId) }

    return sessionsToArchive.size
  }

  private fun archiveAndSaveSessions(sessions: List<DataSession>, archiveName: String) {
    val dataFolder = File(plugin.dataFolder, "datacollection")
    if (!dataFolder.exists()) {
      dataFolder.mkdirs()
    }
    val zipFile = File(dataFolder, "$archiveName.zip")
    try {
      FileOutputStream(zipFile).use { fos ->
        ZipOutputStream(fos).use { zos ->
          for (session in sessions) {
            if (session.recordedTicks.isEmpty()) {
              continue
            }
            val fileName = session.generateFileName()
            val zipEntry = ZipEntry(fileName)
            zos.putNextEntry(zipEntry)
            val writer =
              BufferedWriter(OutputStreamWriter(zos, StandardCharsets.UTF_8), DEFAULT_BUFFER_SIZE)
            session.writeCsv(writer)
            writer.flush()
            zos.closeEntry()
          }
        }
      }
    } catch (e: IOException) {
      plugin.logger.severe("Failed to create data collection archive: ${e.message}")
    }
  }

  fun getSession(uuid: UUID): DataSession? {
    return activeSessions[uuid]
  }
}
