package space.kaelus.sloth.ai

import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import space.kaelus.sloth.data.TickData
import space.kaelus.sloth.flatbuffers.TickDataSequence

class FlatBuffersAiSerializerTest {

  private val serializer = FlatBuffersAiSerializer()

  private data class TickParams(
    val dYaw: Float = 0f,
    val dPitch: Float = 0f,
    val aYaw: Float = 0f,
    val aPitch: Float = 0f,
    val jYaw: Float = 0f,
    val jPitch: Float = 0f,
    val gYaw: Float = 0f,
    val gPitch: Float = 0f,
  )

  private fun mockTick(params: TickParams = TickParams()): TickData = mockk {
    every { deltaYaw } returns params.dYaw
    every { deltaPitch } returns params.dPitch
    every { accelYaw } returns params.aYaw
    every { accelPitch } returns params.aPitch
    every { jerkYaw } returns params.jYaw
    every { jerkPitch } returns params.jPitch
    every { gcdErrorYaw } returns params.gYaw
    every { gcdErrorPitch } returns params.gPitch
  }

  @Test
  fun `serialize single tick produces valid flatbuffer`() {
    val tick = mockTick(TickParams(dYaw = 1.5f, dPitch = -2.0f, aYaw = 0.1f, aPitch = 0.2f))
    val ticks = arrayOf(tick)

    val buffer = serializer.serialize(ticks, 1)
    assertTrue(buffer.remaining() > 0)

    val sequence = TickDataSequence.getRootAsTickDataSequence(buffer)
    assertEquals(1, sequence.ticksLength())
    val decoded = sequence.ticks(0)!!
    assertEquals(1.5f, decoded.deltaYaw())
    assertEquals(-2.0f, decoded.deltaPitch())
    assertEquals(0.1f, decoded.accelYaw())
    assertEquals(0.2f, decoded.accelPitch())
  }

  @Test
  fun `serialize multiple ticks preserves order and values`() {
    val tick0 = mockTick(TickParams(dYaw = 1.0f, dPitch = 2.0f))
    val tick1 = mockTick(TickParams(dYaw = 3.0f, dPitch = 4.0f))
    val tick2 = mockTick(TickParams(dYaw = 5.0f, dPitch = 6.0f))
    val ticks = arrayOf(tick0, tick1, tick2)

    val buffer = serializer.serialize(ticks, 3)
    val sequence = TickDataSequence.getRootAsTickDataSequence(buffer)
    assertEquals(3, sequence.ticksLength())

    assertEquals(1.0f, sequence.ticks(0)!!.deltaYaw())
    assertEquals(2.0f, sequence.ticks(0)!!.deltaPitch())
    assertEquals(3.0f, sequence.ticks(1)!!.deltaYaw())
    assertEquals(4.0f, sequence.ticks(1)!!.deltaPitch())
    assertEquals(5.0f, sequence.ticks(2)!!.deltaYaw())
    assertEquals(6.0f, sequence.ticks(2)!!.deltaPitch())
  }

  @Test
  fun `serialize uses count not array length`() {
    val tick0 = mockTick(TickParams(dYaw = 10.0f))
    val tick1 = mockTick(TickParams(dYaw = 20.0f))
    val tick2 = mockTick(TickParams(dYaw = 30.0f))
    val ticks = arrayOf(tick0, tick1, tick2)

    // Only serialize first 2 even though array has 3
    val buffer = serializer.serialize(ticks, 2)
    val sequence = TickDataSequence.getRootAsTickDataSequence(buffer)
    assertEquals(2, sequence.ticksLength())
    assertEquals(10.0f, sequence.ticks(0)!!.deltaYaw())
    assertEquals(20.0f, sequence.ticks(1)!!.deltaYaw())
  }

  @Test
  fun `serialize all 8 fields correctly`() {
    val tick =
      mockTick(
        TickParams(
          dYaw = 1.0f,
          dPitch = 2.0f,
          aYaw = 3.0f,
          aPitch = 4.0f,
          jYaw = 5.0f,
          jPitch = 6.0f,
          gYaw = 7.0f,
          gPitch = 8.0f,
        )
      )
    val buffer = serializer.serialize(arrayOf(tick), 1)
    val decoded = TickDataSequence.getRootAsTickDataSequence(buffer).ticks(0)!!

    assertEquals(1.0f, decoded.deltaYaw())
    assertEquals(2.0f, decoded.deltaPitch())
    assertEquals(3.0f, decoded.accelYaw())
    assertEquals(4.0f, decoded.accelPitch())
    assertEquals(5.0f, decoded.jerkYaw())
    assertEquals(6.0f, decoded.jerkPitch())
    assertEquals(7.0f, decoded.gcdErrorYaw())
    assertEquals(8.0f, decoded.gcdErrorPitch())
  }

  @Test
  fun `serialize is reusable across calls`() {
    val tick1 = mockTick(TickParams(dYaw = 100.0f))
    val tick2 = mockTick(TickParams(dYaw = 200.0f))

    val buf1 = serializer.serialize(arrayOf(tick1), 1)
    val buf2 = serializer.serialize(arrayOf(tick2), 1)

    val seq1 = TickDataSequence.getRootAsTickDataSequence(buf1)
    val seq2 = TickDataSequence.getRootAsTickDataSequence(buf2)

    assertEquals(100.0f, seq1.ticks(0)!!.deltaYaw())
    assertEquals(200.0f, seq2.ticks(0)!!.deltaYaw())
  }
}
