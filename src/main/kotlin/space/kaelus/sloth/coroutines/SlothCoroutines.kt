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
package space.kaelus.sloth.coroutines

import java.io.Closeable
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import space.kaelus.sloth.scheduler.SchedulerService

class SlothCoroutines(private val scheduler: SchedulerService, private val logger: Logger) :
  Closeable {
  val main: CoroutineDispatcher =
    object : CoroutineDispatcher() {
      override fun dispatch(context: CoroutineContext, block: Runnable) {
        scheduler.runSync(block)
      }
    }

  val async: CoroutineDispatcher =
    object : CoroutineDispatcher() {
      override fun dispatch(context: CoroutineContext, block: Runnable) {
        scheduler.runAsync(block)
      }
    }

  private val job = SupervisorJob()
  private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
    logger.log(Level.SEVERE, "Uncaught coroutine error", throwable)
  }

  val scope: CoroutineScope = CoroutineScope(job + async + exceptionHandler)

  override fun close() {
    job.cancel()
  }
}
