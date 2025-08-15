/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2025 KaelusMC
 *
 * This file contains code derived from GrimAC.
 * The original authors of GrimAC are credited below.
 *
 * Copyright (c) 2021-2025 GrimAC, DefineOutside and contributors.
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
package space.kaelus.sloth.checks;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import space.kaelus.sloth.checks.impl.ai.AICheck;
import space.kaelus.sloth.checks.impl.ai.ActionManager;
import space.kaelus.sloth.checks.impl.ai.DataCollectorCheck;
import space.kaelus.sloth.checks.impl.aim.AimProcessor;
import space.kaelus.sloth.checks.impl.misc.ClientBrand;
import space.kaelus.sloth.checks.type.PacketCheck;
import space.kaelus.sloth.checks.type.RotationCheck;
import space.kaelus.sloth.player.SlothPlayer;
import space.kaelus.sloth.utils.update.RotationUpdate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class CheckManager {
    private final List<RotationCheck> rotationChecks = new ArrayList<>();
    private final List<PacketCheck> packetChecks = new ArrayList<>();

    public CheckManager(SlothPlayer player) {
        rotationChecks.add(new AimProcessor(player));

        packetChecks.add(new ActionManager(player));
        packetChecks.add(new AICheck(player));
        packetChecks.add(new DataCollectorCheck(player));
        packetChecks.add(new ClientBrand(player));
    }

    public void onRotationUpdate(RotationUpdate update) {
        for (RotationCheck check : rotationChecks) {
            check.process(update);
        }
    }

    public void onPacketReceive(PacketReceiveEvent event) {
        for (PacketCheck check : packetChecks) {
            check.onPacketReceive(event);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractCheck> T getCheck(Class<T> clazz) {
        for (AbstractCheck check : getAllChecks()) {
            if (clazz.isInstance(check)) {
                return (T) check;
            }
        }
        return null;
    }

    public Collection<AbstractCheck> getAllChecks() {
        return Stream.concat(rotationChecks.stream(), packetChecks.stream())
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
}