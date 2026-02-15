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
package space.kaelus.sloth

import com.github.retrooper.packetevents.PacketEvents
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import space.kaelus.sloth.di.slothModules

class SlothAC : JavaPlugin() {
  private var core: SlothCore? = null

  override fun onLoad() {
    try {
      PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
      PacketEvents.getAPI()
        .settings
        .checkForUpdates(false)
        .debug(false)
        .fullStackTrace(false)
        .kickOnPacketException(false)
        .reEncodeByDefault(false)
      PacketEvents.getAPI().load()
    } catch (e: Exception) {
      logger.severe("Failed to load PacketEvents.")
      e.printStackTrace()
    }
  }

  override fun onEnable() {
    val koinApp = startKoin { modules(slothModules(this@SlothAC)) }
    core = koinApp.koin.get()
    core?.enable()
  }

  override fun onDisable() {
    core?.disable()
    stopKoin()
  }

  fun onReload() {
    core?.reload()
  }
}
