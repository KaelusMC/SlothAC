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
package space.kaelus.sloth.world;

import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import space.kaelus.sloth.player.SlothPlayer;

public class CompensatedWorld {
  private final SlothPlayer player;
  public final Long2ObjectMap<Column> chunks;

  public CompensatedWorld(SlothPlayer player) {
    this.player = player;
    this.chunks = new Long2ObjectOpenHashMap<>();
  }

  public static long chunkPositionToLong(int x, int z) {
    return ((long) x & 0xFFFFFFFFL) << 32L | ((long) z & 0xFFFFFFFFL);
  }

  public void addToCache(Column chunk, int chunkX, int chunkZ) {
    long pos = chunkPositionToLong(chunkX, chunkZ);
    player
        .getLatencyUtils()
        .addRealTimeTask(player.getLastTransactionSent().get(), () -> chunks.put(pos, chunk));
  }

  public Column getChunk(int chunkX, int chunkZ) {
    return chunks.get(chunkPositionToLong(chunkX, chunkZ));
  }

  public WrappedBlockState getBlock(int x, int y, int z) {
    Column column = getChunk(x >> 4, z >> 4);
    if (column == null || y < -64 || y > 319) {
      return WrappedBlockState.getByGlobalId(0);
    }
    int sectionIndex = (y >> 4) - (-4);
    if (sectionIndex < 0 || sectionIndex >= column.chunks().length) {
      return WrappedBlockState.getByGlobalId(0);
    }
    BaseChunk chunk = column.chunks()[sectionIndex];
    if (chunk != null) {
      return chunk.get(x & 15, y & 15, z & 15);
    }
    return WrappedBlockState.getByGlobalId(0);
  }

  public void clear() {
    chunks.clear();
  }
}
