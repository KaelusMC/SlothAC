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
package space.kaelus.sloth.api.internal

import java.util.Optional
import java.util.UUID
import space.kaelus.sloth.ai.AiService
import space.kaelus.sloth.api.model.AiSnapshot
import space.kaelus.sloth.api.service.AiApi
import space.kaelus.sloth.checks.impl.ai.AiCheck
import space.kaelus.sloth.player.PlayerDataManager

class AiApiImpl(
  private val aiService: AiService,
  private val playerDataManager: PlayerDataManager,
) : AiApi {
  override fun isEnabled(): Boolean = aiService.isEnabled

  override fun getSnapshot(playerId: UUID): Optional<AiSnapshot> {
    val slothPlayer = playerDataManager.getPlayer(playerId) ?: return Optional.empty()
    val aiCheck = slothPlayer.checkManager.getCheck(AiCheck::class.java) ?: return Optional.empty()
    return Optional.of(
      AiSnapshot(
        aiCheck.lastProbability,
        aiCheck.buffer,
        slothPlayer.combat.damageMultiplier,
        aiCheck.prob90,
      )
    )
  }
}
