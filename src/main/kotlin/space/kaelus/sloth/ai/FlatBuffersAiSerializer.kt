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

import com.google.flatbuffers.FlatBufferBuilder
import java.nio.ByteBuffer
import space.kaelus.sloth.data.TickData
import space.kaelus.sloth.flatbuffers.TickDataSequence

class FlatBuffersAiSerializer : AiSerializer {
  override fun serialize(ticks: Array<TickData>, count: Int): ByteBuffer {
    val builder = BUILDER.get()
    builder.clear()

    var tickOffsets = OFFSETS_BUFFER.get()
    if (tickOffsets.size != count) {
      tickOffsets = IntArray(count)
      OFFSETS_BUFFER.set(tickOffsets)
    }

    for (i in count - 1 downTo 0) {
      val tick = ticks[i]
      space.kaelus.sloth.flatbuffers.TickData.startTickData(builder)
      space.kaelus.sloth.flatbuffers.TickData.addDeltaYaw(builder, tick.deltaYaw)
      space.kaelus.sloth.flatbuffers.TickData.addDeltaPitch(builder, tick.deltaPitch)
      space.kaelus.sloth.flatbuffers.TickData.addAccelYaw(builder, tick.accelYaw)
      space.kaelus.sloth.flatbuffers.TickData.addAccelPitch(builder, tick.accelPitch)
      space.kaelus.sloth.flatbuffers.TickData.addJerkYaw(builder, tick.jerkYaw)
      space.kaelus.sloth.flatbuffers.TickData.addJerkPitch(builder, tick.jerkPitch)
      space.kaelus.sloth.flatbuffers.TickData.addGcdErrorYaw(builder, tick.gcdErrorYaw)
      space.kaelus.sloth.flatbuffers.TickData.addGcdErrorPitch(builder, tick.gcdErrorPitch)
      tickOffsets[i] = space.kaelus.sloth.flatbuffers.TickData.endTickData(builder)
    }

    val ticksVector = TickDataSequence.createTicksVector(builder, tickOffsets)

    TickDataSequence.startTickDataSequence(builder)
    TickDataSequence.addTicks(builder, ticksVector)
    val sequenceOffset = TickDataSequence.endTickDataSequence(builder)
    builder.finish(sequenceOffset)

    return ByteBuffer.wrap(builder.sizedByteArray())
  }

  companion object {
    private val BUILDER: ThreadLocal<FlatBufferBuilder> =
      ThreadLocal.withInitial { FlatBufferBuilder(4096) }
    private val OFFSETS_BUFFER: ThreadLocal<IntArray> = ThreadLocal.withInitial { IntArray(0) }
  }
}
