package space.kaelus.sloth.ai

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test

class JacksonAiResponseParserTest {
  private val parser = JacksonAiResponseParser()

  @Test
  fun `parses numeric probability`() {
    val response = parser.parse("""{"probability":0.93}""")
    assertEquals(0.93, response.probability)
  }

  @Test
  fun `parses textual probability`() {
    val response = parser.parse("""{"probability":"0.75"}""")
    assertEquals(0.75, response.probability)
  }

  @Test
  fun `throws for missing probability`() {
    assertFailsWith<IllegalArgumentException> { parser.parse("""{"details":{"sequence":10}}""") }
  }

  @Test
  fun `throws for invalid probability type`() {
    assertFailsWith<IllegalArgumentException> { parser.parse("""{"probability":{"value":0.5}}""") }
  }
}
