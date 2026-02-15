package space.kaelus.sloth.ai

import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import space.kaelus.sloth.server.AIServerProvider

class DefaultAiServiceParseSequenceTest {

  private val service =
    DefaultAiService(
      transportProvider = mockk<AIServerProvider>(relaxed = true),
      serializer = mockk(relaxed = true),
      parser = mockk(relaxed = true),
    )

  @Test
  fun `parses sequence from valid JSON body`() {
    val body = """{"details":{"sequence":42}}"""
    assertEquals(42, service.parseSequence(body))
  }

  @Test
  fun `parses sequence with extra fields`() {
    val body = """{"details":{"sequence":7,"other":"value"},"status":"error"}"""
    assertEquals(7, service.parseSequence(body))
  }

  @Test
  fun `returns null for null input`() {
    assertNull(service.parseSequence(null))
  }

  @Test
  fun `returns null for blank input`() {
    assertNull(service.parseSequence(""))
    assertNull(service.parseSequence("   "))
  }

  @Test
  fun `returns null when no details object`() {
    val body = """{"error":"something"}"""
    assertNull(service.parseSequence(body))
  }

  @Test
  fun `returns null when details has no sequence`() {
    val body = """{"details":{"message":"no sequence here"}}"""
    assertNull(service.parseSequence(body))
  }

  @Test
  fun `returns null for invalid JSON`() {
    assertNull(service.parseSequence("not json at all"))
  }

  @Test
  fun `returns null when details is not an object`() {
    val body = """{"details":"just a string"}"""
    assertNull(service.parseSequence(body))
  }

  @Test
  fun `parses zero sequence`() {
    val body = """{"details":{"sequence":0}}"""
    assertEquals(0, service.parseSequence(body))
  }

  @Test
  fun `parses negative sequence`() {
    val body = """{"details":{"sequence":-1}}"""
    assertEquals(-1, service.parseSequence(body))
  }

  @Test
  fun `parses large sequence number`() {
    val body = """{"details":{"sequence":999999}}"""
    assertEquals(999999, service.parseSequence(body))
  }
}
