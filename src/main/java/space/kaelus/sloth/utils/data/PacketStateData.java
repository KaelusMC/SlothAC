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
package space.kaelus.sloth.utils.data;

import com.github.retrooper.packetevents.util.Vector3d;
import lombok.Getter;

@Getter
public class PacketStateData {
    public boolean packetPlayerOnGround = false;
    public boolean lastPacketWasTeleport = false;
    public boolean lastPacketWasServerRotation = false;
    public boolean cancelDuplicatePacket = true;
    public boolean lastPacketWasOnePointSeventeenDuplicate = false;
    public boolean ignoreDuplicatePacketRotation = true;
    public Vector3d lastClaimedPosition = new Vector3d(0, 0, 0);
}