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
import java.util.*;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.alert.AlertManager;
import space.kaelus.sloth.checks.impl.ai.AICheck;
import space.kaelus.sloth.checks.impl.ai.ActionManager;
import space.kaelus.sloth.checks.impl.ai.DataCollectorCheck;
import space.kaelus.sloth.checks.impl.ai.DataCollectorManager;
import space.kaelus.sloth.checks.impl.aim.AimProcessor;
import space.kaelus.sloth.checks.impl.misc.ClientBrand;
import space.kaelus.sloth.checks.type.PacketCheck;
import space.kaelus.sloth.checks.type.RotationCheck;
import space.kaelus.sloth.config.ConfigManager;
import space.kaelus.sloth.integration.WorldGuardManager;
import space.kaelus.sloth.player.SlothPlayer;
import space.kaelus.sloth.server.AIServerProvider;
import space.kaelus.sloth.utils.update.RotationUpdate;

public class CheckManager {
  private final List<RotationCheck> rotationChecks = new ArrayList<>();
  private final List<PacketCheck> packetChecks = new ArrayList<>();

  private final Map<Class<? extends AbstractCheck>, AbstractCheck> checks = new HashMap<>();

  public CheckManager(
      SlothPlayer player,
      SlothAC plugin,
      ConfigManager configManager,
      DataCollectorManager dataCollectorManager,
      AIServerProvider aiServerProvider,
      WorldGuardManager worldGuardManager,
      AlertManager alertManager) {

    registerCheck(new AimProcessor(player));
    registerCheck(new ActionManager(player, configManager));
    registerCheck(
        new AICheck(
            player, plugin, aiServerProvider, configManager, worldGuardManager, alertManager));
    registerCheck(new DataCollectorCheck(player, dataCollectorManager, plugin));
    registerCheck(new ClientBrand(player, configManager, alertManager));
  }

  private void registerCheck(AbstractCheck check) {
    checks.put(check.getClass(), check);

    if (check instanceof RotationCheck rotationCheck) {
      rotationChecks.add(rotationCheck);
    }

    if (check instanceof PacketCheck packetCheck) {
      packetChecks.add(packetCheck);
    }
  }

  public void reloadChecks() {
    for (AbstractCheck check : checks.values()) {
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
  public <T extends AbstractCheck> T getCheck(Class<T> clazz) {
    return (T) checks.get(clazz);
  }

  public Collection<AbstractCheck> getAllChecks() {
    return checks.values();
  }
}
