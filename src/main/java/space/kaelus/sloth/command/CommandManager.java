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

import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.alert.AlertManager;
import space.kaelus.sloth.checks.impl.ai.DataCollectorManager;
import space.kaelus.sloth.config.ConfigManager;
import space.kaelus.sloth.config.LocaleManager;
import space.kaelus.sloth.database.DatabaseManager;
import space.kaelus.sloth.player.ExemptManager;
import space.kaelus.sloth.player.PlayerDataManager;
import space.kaelus.sloth.sender.Sender;
import space.kaelus.sloth.sender.SenderFactory;

public class CommandManager {

  public CommandManager(
      SlothAC plugin,
      AlertManager alertManager,
      DataCollectorManager dataCollectorManager,
      DatabaseManager databaseManager,
      ConfigManager configManager,
      LocaleManager localeManager,
      PlayerDataManager playerDataManager,
      ExemptManager exemptManager) {

    LegacyPaperCommandManager<Sender> cloudManager = setupCloud(plugin);
    if (cloudManager != null) {
      CommandRegister.registerCommands(
          cloudManager,
          plugin,
          alertManager,
          dataCollectorManager,
          databaseManager,
          configManager,
          localeManager,
          playerDataManager,
          exemptManager);
    }
  }

  private LegacyPaperCommandManager<Sender> setupCloud(SlothAC plugin) {
    SenderFactory senderFactory = new SenderFactory(plugin);
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
