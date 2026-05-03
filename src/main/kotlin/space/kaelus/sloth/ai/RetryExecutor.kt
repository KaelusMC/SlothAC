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
package space.kaelus.sloth.ai

import java.util.concurrent.CompletableFuture
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random
import space.kaelus.sloth.scheduler.SchedulerService

class RetryExecutor(private val scheduler: SchedulerService, private val config: RetryConfig) {
  fun <T> execute(
    operation: () -> CompletableFuture<T>,
    shouldRetry: (Throwable) -> Boolean,
  ): CompletableFuture<T> {
    val result = CompletableFuture<T>()
    attempt(1, operation, shouldRetry, result)
    return result
  }

  private fun <T> attempt(
    attemptNum: Int,
    operation: () -> CompletableFuture<T>,
    shouldRetry: (Throwable) -> Boolean,
    result: CompletableFuture<T>,
  ) {
    operation().whenComplete { value, throwable ->
      if (throwable == null) {
        result.complete(value)
        return@whenComplete
      }
      val cause = unwrap(throwable)
      if (attemptNum >= config.maxAttempts || !shouldRetry(cause)) {
        result.completeExceptionally(cause)
        return@whenComplete
      }
      val delayMs = computeDelayMs(attemptNum).coerceAtLeast(0L)
      scheduler.runLaterAsync({ attempt(attemptNum + 1, operation, shouldRetry, result) }, delayMs)
    }
  }

  private fun computeDelayMs(attemptNum: Int): Long {
    val base = (config.initialDelayMs.toDouble() * config.multiplier.pow(attemptNum - 1)).toLong()
    val capped = min(base, config.maxDelayMs)
    if (config.jitter <= 0.0) return capped
    val jitterRange = (capped * config.jitter).toLong().coerceAtLeast(1L)
    return (capped + Random.nextLong(-jitterRange, jitterRange + 1)).coerceAtLeast(0L)
  }

  private fun unwrap(throwable: Throwable): Throwable {
    return if (throwable is java.util.concurrent.CompletionException && throwable.cause != null) {
      throwable.cause!!
    } else {
      throwable
    }
  }

  data class RetryConfig(
    val maxAttempts: Int,
    val initialDelayMs: Long,
    val maxDelayMs: Long,
    val multiplier: Double,
    val jitter: Double,
  )
}
