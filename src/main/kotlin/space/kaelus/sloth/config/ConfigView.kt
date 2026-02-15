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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package space.kaelus.sloth.config

import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException

class ConfigView(private val rootNode: CommentedConfigurationNode) {
  fun node(path: String): ConfigurationNode = rootNode.node(*path.split('.').toTypedArray())

  fun root(): CommentedConfigurationNode = rootNode

  fun getString(path: String, def: String): String = node(path).getString(def)

  fun getBoolean(path: String, def: Boolean): Boolean = node(path).getBoolean(def)

  fun getInt(path: String, def: Int): Int = node(path).getInt(def)

  fun getLong(path: String, def: Long): Long = node(path).getLong(def)

  fun getDouble(path: String, def: Double): Double = node(path).getDouble(def)

  fun getStringList(path: String): List<String> {
    return try {
      node(path).getList(String::class.java) ?: emptyList()
    } catch (ex: SerializationException) {
      emptyList()
    }
  }

  fun getStringListMap(path: String): Map<String, List<String>> {
    val sectionNode = node(path)
    if (sectionNode.virtual() || !sectionNode.isMap) return emptyMap()
    val result = mutableMapOf<String, List<String>>()
    for ((key, child) in sectionNode.childrenMap()) {
      val keyStr = key.toString()
      val list =
        try {
          child.getList(String::class.java) ?: emptyList()
        } catch (ex: SerializationException) {
          emptyList()
        }
      result[keyStr] = list
    }
    return result
  }
}
