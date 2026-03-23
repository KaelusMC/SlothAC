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
package space.kaelus.sloth.database

import java.util.concurrent.atomic.AtomicReference

internal enum class DatabaseMode {
  PERSISTENT,
  DEGRADED_IN_MEMORY,
}

internal class DatabaseHealth {
  private val modeRef = AtomicReference(DatabaseMode.PERSISTENT)

  @Volatile private var failure: Throwable? = null

  val mode: DatabaseMode
    get() = modeRef.get()

  val failureCause: Throwable?
    get() = failure

  fun isPersistentAvailable(): Boolean = mode == DatabaseMode.PERSISTENT

  fun markPersistent() {
    failure = null
    modeRef.set(DatabaseMode.PERSISTENT)
  }

  fun markDegraded(cause: Throwable) {
    failure = cause
    modeRef.set(DatabaseMode.DEGRADED_IN_MEMORY)
  }
}
