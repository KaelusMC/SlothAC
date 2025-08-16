/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2025 KaelusMC
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
package space.kaelus.sloth.checks.impl.ai;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.checks.Check;
import space.kaelus.sloth.checks.CheckData;
import space.kaelus.sloth.checks.type.PacketCheck;
import space.kaelus.sloth.entity.PacketEntity;
import space.kaelus.sloth.player.SlothPlayer;

@CheckData(name = "ActionManager_Internal")
public class ActionManager extends Check implements PacketCheck {

    public ActionManager(SlothPlayer player) {
        super(player);
        int sequence = SlothAC.getInstance().getConfigManager().getAiSequence();
        player.ticksSinceAttack = sequence + 1;
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity action = new WrapperPlayClientInteractEntity(event);
            if (action.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                PacketEntity entity = slothPlayer.getCompensatedEntities().getEntity(action.getEntityId());
                if (entity != null && entity.isPlayer) {
                    slothPlayer.ticksSinceAttack = 0;
                }
            }
        } else if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            slothPlayer.ticksSinceAttack++;
        }
    }
}