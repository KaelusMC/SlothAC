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
package space.kaelus.sloth.redis

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.junit.jupiter.api.Test

class CrossServerAlertTest {
  private val mapper = ObjectMapper()
  private val gson = GsonComponentSerializer.gson()

  @Test
  fun `payload round-trips through jackson`() {
    val original = CrossServerAlert("origin-1", "Lobby", "REGULAR", """{"text":"hi"}""")

    val json = mapper.writeValueAsString(original)
    val restored = mapper.readValue(json, CrossServerAlert::class.java)

    assertEquals(original, restored)
  }

  @Test
  fun `component survives the gson round-trip inside the payload`() {
    val component = MiniMessage.miniMessage().deserialize("<red>Player</red> <gray>flagged</gray>")
    val payload = CrossServerAlert("o", "Lobby", "SUSPICIOUS", gson.serialize(component))

    val json = mapper.writeValueAsString(payload)
    val restored = mapper.readValue(json, CrossServerAlert::class.java)
    val restoredComponent = gson.deserialize(restored.component)

    assertEquals(gson.serialize(component), gson.serialize(restoredComponent))
  }
}
