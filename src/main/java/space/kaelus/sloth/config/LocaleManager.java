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
package space.kaelus.sloth.config;

import java.io.File;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.utils.Message;

@Singleton
public class LocaleManager {

  private final SlothAC plugin;
  private final ConfigManager configManager;
  private FileConfiguration messagesConfig;

  @Inject
  public LocaleManager(SlothAC plugin, ConfigManager configManager) {
    this.plugin = plugin;
    this.configManager = configManager;
    reload();
  }

  public void reload() {
    String locale = configManager.getConfig().getString("locale", "en");
    File messagesDir = new File(plugin.getDataFolder(), "messages");
    if (!messagesDir.exists()) {
      messagesDir.mkdirs();
    }

    saveDefaultLocale("en");
    if (!locale.equalsIgnoreCase("en")) {
      saveDefaultLocale(locale);
    }

    File messagesFile = new File(messagesDir, "messages_" + locale + ".yml");
    if (!messagesFile.exists()) {
      plugin.getLogger().warning("Locale " + locale + " not found.");
      messagesFile = new File(messagesDir, "messages_en.yml");
    }

    this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

    File defaultFile = new File(messagesDir, "messages_en.yml");
    if (defaultFile.exists()) {
      this.messagesConfig.setDefaults(YamlConfiguration.loadConfiguration(defaultFile));
    }
  }

  private void saveDefaultLocale(String locale) {
    File dir = new File(plugin.getDataFolder(), "messages");
    File file = new File(dir, "messages_" + locale + ".yml");
    if (!file.exists()) {
      plugin.saveResource("messages/messages_" + locale + ".yml", false);
    }
  }

  public String getRawMessage(Message key) {
    return messagesConfig.getString(key.getPath(), "Missing message: " + key.getPath());
  }

  public List<String> getRawMessageList(Message key) {
    return messagesConfig.getStringList(key.getPath());
  }
}
