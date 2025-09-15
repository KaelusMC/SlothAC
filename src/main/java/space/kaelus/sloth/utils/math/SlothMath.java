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
package space.kaelus.sloth.utils.math;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SlothMath {
  public static final double MINIMUM_DIVISOR = ((Math.pow(0.2f, 3) * 8) * 0.15) - 1e-3;

  public static double gcd(double a, double b) {
    if (a == 0) return 0;

    // Make sure a is larger than b
    if (a < b) {
      double temp = a;
      a = b;
      b = temp;
    }

    while (b > MINIMUM_DIVISOR) { // Minimum minecraft sensitivity
      double temp = a - (Math.floor(a / b) * b);
      a = b;
      b = temp;
    }

    return a;
  }
}
