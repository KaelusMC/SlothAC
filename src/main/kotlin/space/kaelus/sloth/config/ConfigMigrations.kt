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

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.NodePath
import org.spongepowered.configurate.transformation.ConfigurationTransformation

object ConfigMigrations {
  const val LATEST_VERSION = 1

  private val VERSIONED: ConfigurationTransformation.Versioned =
    ConfigurationTransformation.versionedBuilder()
      .versionKey("config-version")
      .addVersion(LATEST_VERSION, initialTransform())
      .build()

  fun apply(node: ConfigurationNode): Boolean {
    val before = VERSIONED.version(node)
    VERSIONED.apply(node)
    return VERSIONED.version(node) != before
  }

  /** Migration from unknown (-1) to version 1: add ai.continuous default. */
  private fun initialTransform(): ConfigurationTransformation {
    return ConfigurationTransformation.builder()
      .addAction(NodePath.path("ai")) { _, value ->
        if (value.node("continuous").virtual()) {
          value.node("continuous").set(false)
        }
        null
      }
      .build()
  }
}
