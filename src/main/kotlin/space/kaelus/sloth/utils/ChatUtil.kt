/*
 * This file is part of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2026 GrimAC, DefineOutside and contributors
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
package space.kaelus.sloth.utils

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

object ChatUtil {
  private val LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection()
  private val PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText()

  @JvmStatic
  fun translateAlternateColorCodes(altColorChar: Char, textToTranslate: String): String {
    val chars = textToTranslate.toCharArray()

    var i = 0
    while (i < chars.size - 1) {
      if (
        chars[i] == altColorChar &&
          "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(chars[i + 1]) > -1
      ) {
        chars[i] = 167.toChar()
        chars[i + 1] = chars[i + 1].lowercaseChar()
      }
      i++
    }

    return String(chars)
  }

  @JvmStatic
  fun stripColor(input: String?): String? {
    if (input == null) {
      return null
    }
    return PLAIN_SERIALIZER.serialize(LEGACY_SERIALIZER.deserialize(input))
  }
}
