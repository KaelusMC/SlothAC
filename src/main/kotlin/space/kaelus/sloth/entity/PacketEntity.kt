/*
 * This file is part of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2026 GrimAC, DefineOutside and contributors
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
package space.kaelus.sloth.entity

import com.github.retrooper.packetevents.protocol.entity.type.EntityType
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import java.util.UUID
import space.kaelus.sloth.player.SlothPlayer

open class PacketEntity(val player: SlothPlayer, val uuid: UUID, val type: EntityType) {
  val isLivingEntity: Boolean = EntityTypes.isTypeInstanceOf(type, EntityTypes.LIVINGENTITY)
  val isPlayer: Boolean = type == EntityTypes.PLAYER
  val isBoat: Boolean = EntityTypes.isTypeInstanceOf(type, EntityTypes.BOAT)

  var riding: PacketEntity? = null
  private val passengers: MutableList<PacketEntity> = ArrayList(0)

  fun inVehicle(): Boolean = riding != null

  fun mount(vehicle: PacketEntity) {
    if (riding != null) {
      eject()
    }
    vehicle.passengers.add(this)
    riding = vehicle
  }

  fun eject() {
    riding?.passengers?.remove(this)
    riding = null
  }
}
