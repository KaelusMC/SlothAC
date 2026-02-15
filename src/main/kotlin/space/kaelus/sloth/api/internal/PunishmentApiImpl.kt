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
package space.kaelus.sloth.api.internal

import java.util.UUID
import java.util.concurrent.CompletableFuture
import space.kaelus.sloth.api.service.PunishmentApi
import space.kaelus.sloth.database.DatabaseManager
import space.kaelus.sloth.database.ViolationDatabase
import space.kaelus.sloth.scheduler.SchedulerService

class PunishmentApiImpl(databaseManager: DatabaseManager, private val scheduler: SchedulerService) :
  PunishmentApi {
  private val database: ViolationDatabase = databaseManager.database

  override fun getViolationLevel(playerId: UUID, groupName: String): CompletableFuture<Int> {
    if (groupName.isBlank()) {
      return CompletableFuture.completedFuture(0)
    }
    val future = CompletableFuture<Int>()
    scheduler.runAsync {
      try {
        future.complete(database.getViolationLevel(playerId, groupName))
      } catch (e: Exception) {
        future.completeExceptionally(e)
      }
    }
    return future
  }

  override fun resetViolationLevel(playerId: UUID, groupName: String): CompletableFuture<Void> {
    if (groupName.isBlank()) {
      return CompletableFuture.completedFuture(null)
    }
    val future = CompletableFuture<Void>()
    scheduler.runAsync {
      try {
        database.resetViolationLevel(playerId, groupName)
        future.complete(null)
      } catch (e: Exception) {
        future.completeExceptionally(e)
      }
    }
    return future
  }
}
