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
package space.kaelus.sloth.utils.lists;

import it.unimi.dsi.fastutil.doubles.Double2IntMap;
import it.unimi.dsi.fastutil.doubles.Double2IntOpenHashMap;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import lombok.Getter;

public class RunningMode {
  private static final double threshold = 1e-3;
  private final Queue<Double> addList;
  private final Double2IntMap popularityMap = new Double2IntOpenHashMap();
  @Getter private final int maxSize;

  @Getter private double modeValue;
  @Getter private int modeCount;

  public RunningMode(int maxSize) {
    if (maxSize == 0) throw new IllegalArgumentException("There's no mode to a size 0 list!");
    this.addList = new ArrayBlockingQueue<>(maxSize);
    this.maxSize = maxSize;
  }

  public int size() {
    return addList.size();
  }

  public void add(double value) {
    pop();

    for (Double2IntMap.Entry entry : popularityMap.double2IntEntrySet()) {
      if (Math.abs(entry.getDoubleKey() - value) < threshold) {
        entry.setValue(entry.getIntValue() + 1);
        addList.add(entry.getDoubleKey());
        return;
      }
    }

    popularityMap.put(value, 1);
    addList.add(value);
  }

  private void pop() {
    if (addList.size() >= maxSize) {
      double type = addList.poll();
      int popularity = popularityMap.get(type);
      if (popularity == 1) {
        popularityMap.remove(type);
      } else {
        popularityMap.put(type, popularity - 1);
      }
    }
  }

  public void updateMode() {
    int max = 0;
    double mostPopular = 0.0;

    for (Double2IntMap.Entry entry : popularityMap.double2IntEntrySet()) {
      if (entry.getIntValue() > max) {
        max = entry.getIntValue();
        mostPopular = entry.getDoubleKey();
      }
    }

    this.modeValue = mostPopular;
    this.modeCount = max;
  }
}
