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
package space.kaelus.sloth.command;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.sender.Sender;
import space.kaelus.sloth.sender.SenderFactory;

@Singleton
public class CommandManager {
  private final SlothAC plugin;
  private final CommandRegister commandRegister;
  private final BukkitAudiences adventure;

  private LegacyPaperCommandManager<Sender> cloudManager;

  @Inject
  public CommandManager(
      SlothAC plugin, CommandRegister commandRegister, BukkitAudiences adventure) {
    this.plugin = plugin;
    this.commandRegister = commandRegister;
    this.adventure = adventure;
  }

  public void registerCommands() {
    if (cloudManager != null) {
      return;
    }

    LegacyPaperCommandManager<Sender> manager = setupCloud(plugin);
    if (manager == null) {
      return;
    }

    commandRegister.registerCommands(manager);
    this.cloudManager = manager;
  }

  private LegacyPaperCommandManager<Sender> setupCloud(SlothAC plugin) {
    SenderFactory senderFactory = new SenderFactory(adventure);
    LegacyPaperCommandManager<Sender> manager;
    try {
      manager =
          new LegacyPaperCommandManager<>(
              plugin, ExecutionCoordinator.simpleCoordinator(), senderFactory);
    } catch (Exception e) {
      plugin.getLogger().severe("Failed to initialize Cloud Command Manager: " + e.getMessage());
      e.printStackTrace();
      return null;
    }

    if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
      manager.registerBrigadier();
    } else if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
      manager.registerAsynchronousCompletions();
    }

    return manager;
  }
}
