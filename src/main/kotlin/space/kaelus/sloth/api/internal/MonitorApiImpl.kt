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
import space.kaelus.sloth.api.model.MonitorSnapshot
import space.kaelus.sloth.api.service.MonitorApi
import space.kaelus.sloth.checks.impl.ai.AiCheck
import space.kaelus.sloth.player.PlayerDataManager

class MonitorApiImpl(private val playerDataManager: PlayerDataManager) : MonitorApi {
  override fun getSnapshot(playerId: UUID): Optional<MonitorSnapshot> {
    val slothPlayer = playerDataManager.getPlayer(playerId) ?: return Optional.empty()
    val aiCheck = slothPlayer.checkManager.getCheck(AiCheck::class.java) ?: return Optional.empty()
    val ping = slothPlayer.player.ping
    return Optional.of(
      MonitorSnapshot(
        aiCheck.lastProbability,
        aiCheck.buffer,
        ping,
        slothPlayer.combat.damageMultiplier,
        aiCheck.prob90,
      )
    )
  }
}
