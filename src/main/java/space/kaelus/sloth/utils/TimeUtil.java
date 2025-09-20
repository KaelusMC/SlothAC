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
package space.kaelus.sloth.utils;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;
import space.kaelus.sloth.config.LocaleManager;

@UtilityClass
public class TimeUtil {

  public String formatDuration(long millis, LocaleManager lm) {
    if (millis < 0) return "0" + lm.getRawMessage(Message.TIME_SECONDS);

    String d = lm.getRawMessage(Message.TIME_DAYS);
    String h = lm.getRawMessage(Message.TIME_HOURS);
    String m = lm.getRawMessage(Message.TIME_MINUTES);
    String s = lm.getRawMessage(Message.TIME_SECONDS);

    long days = TimeUnit.MILLISECONDS.toDays(millis);
    millis -= TimeUnit.DAYS.toMillis(days);
    long hours = TimeUnit.MILLISECONDS.toHours(millis);
    millis -= TimeUnit.HOURS.toMillis(hours);
    long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
    millis -= TimeUnit.MINUTES.toMillis(minutes);
    long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

    StringBuilder sb = new StringBuilder();
    if (days > 0) sb.append(days).append(d).append(" ");
    if (hours > 0) sb.append(hours).append(h).append(" ");
    if (minutes > 0) sb.append(minutes).append(m).append(" ");
    if (sb.length() == 0 || seconds > 0) {
      sb.append(seconds).append(s);
    }

    return sb.toString().trim();
  }

  public String formatTimeAgo(Date date, LocaleManager lm) {
    String ago = lm.getRawMessage(Message.TIME_AGO);
    long durationMillis = System.currentTimeMillis() - date.getTime();

    long days = TimeUnit.MILLISECONDS.toDays(durationMillis);
    if (days > 0) return days + lm.getRawMessage(Message.TIME_DAYS) + ago;

    long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
    if (hours > 0) return hours + lm.getRawMessage(Message.TIME_HOURS) + ago;

    long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis);
    if (minutes > 0) return minutes + lm.getRawMessage(Message.TIME_MINUTES) + ago;

    return TimeUnit.MILLISECONDS.toSeconds(durationMillis)
        + lm.getRawMessage(Message.TIME_SECONDS)
        + ago;
  }
}
