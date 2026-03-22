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
    val body = """{"details":{"expected_sequence":42}}"""
    assertEquals(42, service.parseSequence(body))
  }

  @Test
  fun `parses sequence with extra fields`() {
    val body =
      """{"details":{"expected_sequence":7,"received_sequence":40,"other":"value"},"status":"error"}"""
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
  fun `parses expected_sequence from new protocol`() {
    val body =
      """{"code":"INVALID_SEQUENCE","details":{"expected_sequence":40,"received_sequence":50}}"""
    assertEquals(40, service.parseSequence(body))
  }

  @Test
  fun `returns null when expected_sequence is missing`() {
    val body = """{"details":{"sequence":50}}"""
    assertNull(service.parseSequence(body))
  }

  @Test
  fun `returns null when only error text is present`() {
    val body =
      """{"error":"Invalid sequence length for model sloth_1_preview. Expected 40, got 50"}"""
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
    val body = """{"details":{"expected_sequence":0}}"""
    assertEquals(0, service.parseSequence(body))
  }

  @Test
  fun `parses negative sequence`() {
    val body = """{"details":{"expected_sequence":-1}}"""
    assertEquals(-1, service.parseSequence(body))
  }

  @Test
  fun `parses large sequence number`() {
    val body = """{"details":{"expected_sequence":999999}}"""
    assertEquals(999999, service.parseSequence(body))
  }
}
