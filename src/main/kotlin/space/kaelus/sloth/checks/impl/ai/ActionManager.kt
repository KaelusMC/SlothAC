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
package space.kaelus.sloth.checks.impl.ai

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying
import space.kaelus.sloth.checks.AbstractCheck
import space.kaelus.sloth.checks.CheckData
import space.kaelus.sloth.checks.CheckFactory
import space.kaelus.sloth.checks.type.PacketCheck
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.entity.PacketEntity
import space.kaelus.sloth.player.SlothPlayer

@CheckData(name = "ActionManager_Internal")
class ActionManager(player: SlothPlayer, configManager: ConfigManager) :
  AbstractCheck(player), PacketCheck {
  init {
    val sequence = configManager.aiSequence
    player.combat.ticksSinceAttack = sequence + 1
  }

  interface Factory : CheckFactory {
    override fun create(player: SlothPlayer): ActionManager
  }

  override fun onPacketReceive(event: PacketReceiveEvent) {
    if (event.packetType == PacketType.Play.Client.INTERACT_ENTITY) {
      val action = WrapperPlayClientInteractEntity(event)
      if (action.action == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
        val entity: PacketEntity =
          slothPlayer.compensatedEntities.getEntity(action.entityId) ?: return

        if (entity.isPlayer) {
          slothPlayer.combat.ticksSinceAttack = 0
        }
      }
    } else if (WrapperPlayClientPlayerFlying.isFlying(event.packetType)) {
      slothPlayer.combat.ticksSinceAttack = slothPlayer.combat.ticksSinceAttack + 1
    }
  }
}
