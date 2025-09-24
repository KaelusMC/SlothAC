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
package space.kaelus.sloth.debug;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.config.ConfigManager;

public class DebugManager {
  private final SlothAC plugin;
  private final ConfigManager configManager;
  private final Set<DebugCategory> enabledCategories = EnumSet.noneOf(DebugCategory.class);

  public DebugManager(SlothAC plugin, ConfigManager configManager) {
    this.plugin = plugin;
    this.configManager = configManager;
    reload();
  }

  public void reload() {
    enabledCategories.clear();
    List<String> enabledKeys = configManager.getEnabledDebugCategories();
    for (String key : enabledKeys) {
      try {
        enabledCategories.add(DebugCategory.valueOf(key.toUpperCase()));
      } catch (IllegalArgumentException e) {
        plugin.getLogger().warning("Invalid debug category in config: " + key);
      }
    }
  }

  public boolean isEnabled(DebugCategory category) {
    return enabledCategories.contains(category);
  }

  public void log(DebugCategory category, String message) {
    if (isEnabled(category)) {
      plugin.getLogger().info("[DEBUG | " + category.name() + "] " + message);
    }
  }
}
