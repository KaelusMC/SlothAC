/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2026 KaelusMC
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
package space.kaelus.sloth.config

import java.io.File
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.utils.Message

class LocaleManager(private val plugin: SlothAC, private val configManager: ConfigManager) {
  private var messagesConfig: ConfigView = ConfigView(CommentedConfigurationNode.root())
  private var defaultMessages: ConfigView? = null

  init {
    reload()
  }

  fun reload() {
    val locale = configManager.config.getString("locale", "en")
    val messagesDir = File(plugin.dataFolder, "messages")
    if (!messagesDir.exists()) {
      messagesDir.mkdirs()
    }

    saveDefaultLocale("en")
    if (!"en".equals(locale, ignoreCase = true)) {
      saveDefaultLocale(locale)
    }

    var messagesFile = File(messagesDir, "messages_$locale.yml")
    if (!messagesFile.exists()) {
      plugin.logger.warning("Locale $locale not found.")
      messagesFile = File(messagesDir, "messages_en.yml")
    }

    messagesConfig = loadMessages(messagesFile)

    val defaultFile = File(messagesDir, "messages_en.yml")
    defaultMessages = if (defaultFile.exists()) loadMessages(defaultFile) else null
  }

  private fun saveDefaultLocale(locale: String) {
    val dir = File(plugin.dataFolder, "messages")
    val file = File(dir, "messages_$locale.yml")
    if (!file.exists()) {
      plugin.saveResource("messages/messages_$locale.yml", false)
    }
  }

  fun getRawMessage(key: Message): String {
    val node = resolveNode(key)
    return node.getString("Missing message: ${key.path}")
  }

  fun getRawMessageList(key: Message): List<String> {
    val node = resolveNode(key)
    return try {
      node.getList(String::class.java) ?: emptyList()
    } catch (_: SerializationException) {
      emptyList()
    }
  }

  private fun loadMessages(file: File): ConfigView {
    return try {
      val loader = YamlConfigurationLoader.builder().path(file.toPath()).build()
      ConfigView(loader.load())
    } catch (_: Exception) {
      plugin.logger.warning("Failed to load locale file ${file.name}")
      ConfigView(CommentedConfigurationNode.root())
    }
  }

  private fun resolveNode(key: Message): ConfigurationNode {
    val path = key.path
    val node = messagesConfig.node(path)
    if (node.empty() && defaultMessages != null) {
      return defaultMessages!!.node(path)
    }
    return node
  }
}
