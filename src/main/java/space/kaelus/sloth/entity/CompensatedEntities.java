/*
 * This file is part of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 GrimAC, DefineOutside and contributors
 *
 * GrimAC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GrimAC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package space.kaelus.sloth.entity;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import space.kaelus.sloth.entity.types.*;
import space.kaelus.sloth.player.SlothPlayer;

import java.util.UUID;

public class CompensatedEntities {
    private final SlothPlayer player;
    public final Int2ObjectOpenHashMap<PacketEntity> entityMap = new Int2ObjectOpenHashMap<>();
    public final PacketEntitySelf self;

    public CompensatedEntities(SlothPlayer player) {
        this.player = player;
        this.self = new PacketEntitySelf(player);
    }

    public void addEntity(int entityId, UUID uuid, EntityType type) {
        PacketEntity packetEntity;

        if (type == EntityTypes.PLAYER) {
            packetEntity = new PacketEntityPlayer(player, uuid);
        } else if (EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_HORSE)) {
            packetEntity = new PacketEntityHorse(player, uuid, type);
        } else if (EntityTypes.isTypeInstanceOf(type, EntityTypes.BOAT) || type == EntityTypes.CHICKEN) {
            packetEntity = new PacketEntityTrackXRot(player, uuid, type);
        } else if (
                EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_ARROW)
                        || type == EntityTypes.FIREWORK_ROCKET
                        || type == EntityTypes.ITEM
        ) {
            packetEntity = new PacketEntityUnHittable(player, uuid, type);
        } else if (type == EntityTypes.ARMOR_STAND) {
            packetEntity = new PacketEntityArmorStand(player, uuid, type);
        } else {
            packetEntity = new PacketEntity(player, uuid, type);
        }

        entityMap.put(entityId, packetEntity);
    }

    public PacketEntity getEntity(int entityId) {
        if (entityId == player.getEntityId()) {
            return self;
        }
        return entityMap.get(entityId);
    }

    public void removeEntity(int entityId) {
        entityMap.remove(entityId);
    }

    public void clear() {
        entityMap.clear();
    }
}