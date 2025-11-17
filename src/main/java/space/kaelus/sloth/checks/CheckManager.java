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
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import space.kaelus.sloth.checks.type.PacketCheck;
import space.kaelus.sloth.checks.type.RotationCheck;
import space.kaelus.sloth.player.SlothPlayer;
import space.kaelus.sloth.utils.update.RotationUpdate;

public class CheckManager {
  private final List<RotationCheck> rotationChecks = new ArrayList<>();
  private final List<PacketCheck> packetChecks = new ArrayList<>();

  private final Map<Class<? extends ICheck>, ICheck> checks = new HashMap<>();

  @AssistedInject
  public CheckManager(@Assisted SlothPlayer player, Set<CheckFactory> checkFactories) {
    for (CheckFactory factory : checkFactories) {
      registerCheck(factory.create(player));
    }
  }

  private void registerCheck(ICheck check) {
    checks.put(check.getClass(), check);

    if (check instanceof RotationCheck rotationCheck) {
      rotationChecks.add(rotationCheck);
    }

    if (check instanceof PacketCheck packetCheck) {
      packetChecks.add(packetCheck);
    }
  }

  public void reloadChecks() {
    for (ICheck check : checks.values()) {
      if (check instanceof Reloadable reloadable) {
        reloadable.reload();
      }
    }
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
  public <T extends ICheck> T getCheck(Class<T> clazz) {
    return (T) checks.get(clazz);
  }

  public Collection<ICheck> getAllChecks() {
    return checks.values();
  }

  @AssistedFactory
  public interface Factory {
    CheckManager create(SlothPlayer player);
  }
}
