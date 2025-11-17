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
package space.kaelus.sloth.server;

import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.config.ConfigManager;

@Singleton
public class AIServerProvider implements Supplier<AIServer> {

  private final SlothAC plugin;
  private final ConfigManager configManager;
  private AIServer currentInstance;
  private ApiCooldown apiCooldown;

  @Inject
  public AIServerProvider(SlothAC plugin, ConfigManager configManager) {
    this.plugin = plugin;
    this.configManager = configManager;
    this.reload();
  }

  public void reload() {
    this.apiCooldown = new ApiCooldown(configManager);
    if (configManager.isAiEnabled()) {
      String url = configManager.getAiServerUrl();
      String key = configManager.getAiApiKey();

      if (url == null || url.isEmpty() || key == null || key.equals("API-KEY")) {
        plugin.getLogger().warning("[AICheck] AI is enabled but not configured.");
        this.currentInstance = null;
      } else {
        plugin.getLogger().info("[AICheck] AI Check loaded.");
        this.currentInstance = new AIServer(plugin, url, key, apiCooldown);
      }
    } else {
      plugin.getLogger().info("[AICheck] AI Check disabled.");
      this.currentInstance = null;
    }
  }

  @Override
  public AIServer get() {
    return this.currentInstance;
  }
}
