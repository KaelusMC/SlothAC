/*
 * This file is part of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 GrimAC, DefineOutside and contributors
 *
 * GrimAC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GrimAC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package space.kaelus.sloth.utils;

import java.util.regex.Pattern;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChatUtil {

  private static final Pattern STRIP_COLOR_PATTERN =
      Pattern.compile("(?i)" + '§' + "[0-9A-FK-ORX]");

  public static @NotNull String translateAlternateColorCodes(
      char altColorChar, @NotNull String textToTranslate) {
    char[] b = textToTranslate.toCharArray();

    for (int i = 0; i < b.length - 1; ++i) {
      if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(b[i + 1]) > -1) {
        b[i] = 167;
        b[i + 1] = Character.toLowerCase(b[i + 1]);
      }
    }

    return new String(b);
  }

  @Contract("!null -> !null; null -> null")
  public static @Nullable String stripColor(@Nullable String input) {
    return input == null ? null : STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
  }
}
