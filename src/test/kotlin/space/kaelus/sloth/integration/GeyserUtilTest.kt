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
package space.kaelus.sloth.integration

import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class GeyserUtilTest {

  @Test
  fun `detects Geyser-formatted bedrock UUID`() {
    val bedrock = UUID.fromString("00000000-0000-0000-0009-000000000001")
    assertTrue(GeyserUtil.isBedrockPlayer(bedrock))
  }

  @Test
  fun `does not flag a regular Java UUID`() {
    val java = UUID.fromString("a1b2c3d4-e5f6-4789-abcd-ef0123456789")
    assertFalse(GeyserUtil.isBedrockPlayer(java))
  }

  @Test
  fun `does not flag the nil UUID`() {
    assertFalse(GeyserUtil.isBedrockPlayer(UUID(0L, 0L)))
  }
}
