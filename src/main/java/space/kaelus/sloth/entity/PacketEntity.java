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
 * GNU General Public License for more details
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package space.kaelus.sloth.entity;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import space.kaelus.sloth.player.SlothPlayer;

@Getter
public class PacketEntity {
  private final SlothPlayer player;
  private final UUID uuid;
  private final EntityType type;
  public final boolean isLivingEntity;
  public final boolean isPlayer;
  public final boolean isBoat;

  @Setter private PacketEntity riding = null;
  private final List<PacketEntity> passengers = new ArrayList<>(0);

  public PacketEntity(SlothPlayer player, UUID uuid, EntityType type) {
    this.player = player;
    this.uuid = uuid;
    this.type = type;
    this.isLivingEntity = EntityTypes.isTypeInstanceOf(type, EntityTypes.LIVINGENTITY);
    this.isPlayer = type == EntityTypes.PLAYER;
    this.isBoat = EntityTypes.isTypeInstanceOf(type, EntityTypes.BOAT);
  }

  public boolean inVehicle() {
    return this.riding != null;
  }

  public void mount(PacketEntity vehicle) {
    if (riding != null) {
      eject();
    }
    vehicle.passengers.add(this);
    this.riding = vehicle;
  }

  public void eject() {
    if (riding != null) {
      riding.passengers.remove(this);
    }
    this.riding = null;
  }
}
