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
package space.kaelus.sloth.server;

import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.java.Log;
import space.kaelus.sloth.config.ConfigManager;

@Log
public class ApiCooldown {

  private final AtomicLong nextAttempt = new AtomicLong(0);
  private final AtomicLong currentBackoff;

  private final long initialDuration;
  private final long maxDuration;
  private final double multiplier;

  public ApiCooldown(ConfigManager configManager) {
    this.initialDuration =
        configManager.getConfig().getLong("ai.backoff.initial-duration", 5) * 1000;
    this.maxDuration = configManager.getConfig().getLong("ai.backoff.max-duration", 60) * 1000;
    this.multiplier = configManager.getConfig().getDouble("ai.backoff.multiplier", 2.0);
    this.currentBackoff = new AtomicLong(this.initialDuration);
  }

  public boolean isWaiting() {
    return System.currentTimeMillis() < nextAttempt.get();
  }

  public void recordSuccess() {
    currentBackoff.set(initialDuration);
    nextAttempt.set(0);
  }

  public void recordFailure() {
    long currentDuration = currentBackoff.get();

    nextAttempt.set(System.currentTimeMillis() + currentDuration);

    long newDuration = (long) (currentDuration * multiplier);
    currentBackoff.set(Math.min(newDuration, maxDuration));
  }
}
