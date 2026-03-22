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
