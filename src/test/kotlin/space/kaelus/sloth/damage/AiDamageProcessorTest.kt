package space.kaelus.sloth.damage

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class AiDamageProcessorTest {

  @Test
  fun `below threshold returns multiplier 1`() {
    assertEquals(1.0, AiDamageProcessor.computeMultiplier(0.5, 0.9, 1.0))
  }

  @Test
  fun `at threshold returns multiplier 1`() {
    // ratio = (0.9 - 0.9) / (1.0 - 0.9) = 0.0, reduction = 0.0
    assertEquals(1.0, AiDamageProcessor.computeMultiplier(0.9, 0.9, 1.0), 1e-9)
  }

  @Test
  fun `above threshold reduces damage proportionally`() {
    // ratio = (0.95-0.9)/(1.0-0.9) = 0.5, reduction = min(1.0, 0.5*1.0) = 0.5
    assertEquals(0.5, AiDamageProcessor.computeMultiplier(0.95, 0.9, 1.0), 1e-9)
  }

  @Test
  fun `probability 1_0 with multiplier 1 gives zero damage`() {
    // ratio = (1.0-0.9)/(1.0-0.9) = 1.0, reduction = min(1.0, 1.0) = 1.0
    assertEquals(0.0, AiDamageProcessor.computeMultiplier(1.0, 0.9, 1.0), 1e-9)
  }

  @Test
  fun `reduction capped at 1 when multiplier is high`() {
    // ratio = (0.95-0.9)/(1.0-0.9) = 0.5, reduction = min(1.0, 0.5*5.0) = min(1.0, 2.5) = 1.0
    assertEquals(0.0, AiDamageProcessor.computeMultiplier(0.95, 0.9, 5.0), 1e-9)
  }

  @Test
  fun `low multiplier gives partial reduction`() {
    // ratio = (0.9-0.8)/(1.0-0.8) = 0.5, reduction = min(1.0, 0.5*0.5) = 0.25
    assertEquals(0.75, AiDamageProcessor.computeMultiplier(0.9, 0.8, 0.5), 1e-9)
  }

  @Test
  fun `zero probability returns multiplier 1`() {
    assertEquals(1.0, AiDamageProcessor.computeMultiplier(0.0, 0.9, 1.0))
  }

  @Test
  fun `threshold 0 means any probability reduces damage`() {
    // ratio = (0.5-0.0)/(1.0-0.0) = 0.5, reduction = min(1.0, 0.5*1.0) = 0.5
    assertEquals(0.5, AiDamageProcessor.computeMultiplier(0.5, 0.0, 1.0), 1e-9)
  }
}
