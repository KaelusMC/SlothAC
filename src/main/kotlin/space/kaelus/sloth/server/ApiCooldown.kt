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
package space.kaelus.sloth.server

import java.util.concurrent.atomic.AtomicLong

class ApiCooldown(initialDurationSeconds: Long, maxDurationSeconds: Long, multiplier: Double) {
  private val nextAttempt: AtomicLong = AtomicLong(0)
  private val currentBackoff: AtomicLong

  private val initialDuration: Long
  private val maxDuration: Long
  private val multiplier: Double

  init {
    initialDuration = initialDurationSeconds * 1000
    maxDuration = maxDurationSeconds * 1000
    this.multiplier = multiplier
    currentBackoff = AtomicLong(initialDuration)
  }

  fun isWaiting(): Boolean = System.currentTimeMillis() < nextAttempt.get()

  fun recordSuccess() {
    currentBackoff.set(initialDuration)
    nextAttempt.set(0)
  }

  fun recordFailure() {
    val currentDuration = currentBackoff.get()
    nextAttempt.set(System.currentTimeMillis() + currentDuration)

    val newDuration = (currentDuration * multiplier).toLong()
    currentBackoff.set(kotlin.math.min(newDuration, maxDuration))
  }
}
