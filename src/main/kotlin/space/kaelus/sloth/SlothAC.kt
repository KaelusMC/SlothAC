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

import java.util.logging.Level
import org.bstats.bukkit.Metrics
import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import space.kaelus.sloth.di.slothModules

class SlothAC : JavaPlugin() {
  private var core: SlothCore? = null
  private val packetEventsLoader = PacketEventsLoader(this)
  private var packetEventsLoadFailure: Throwable? = null

  override fun onLoad() {
    packetEventsLoadFailure = runCatching { packetEventsLoader.load() }.exceptionOrNull()
  }

  override fun onEnable() {
    var koinStarted = false
    packetEventsLoadFailure?.let { failure ->
      handleEnableFailure(failure, koinStarted)
      return
    }

    val failure =
      runCatching {
          val koinApp = startKoin { modules(slothModules(this@SlothAC)) }
          koinStarted = true
          core = koinApp.koin.get()
          core?.enable()
          Metrics(this, BSTATS_PLUGIN_ID)
        }
        .exceptionOrNull()

    if (failure != null) {
      handleEnableFailure(failure, koinStarted)
    }
  }

  override fun onDisable() {
    core?.disable()
    core = null
    runCatching { stopKoin() }
    packetEventsLoader.shutdown()
  }

  fun onReload() {
    core?.reload()
  }

  private companion object {
    const val BSTATS_PLUGIN_ID = 30367
  }

  private fun handleEnableFailure(failure: Throwable, koinStarted: Boolean) {
    logger.log(Level.SEVERE, "Sloth failed to start and will disable itself safely.", failure)
    runCatching { core?.disable() }
    core = null
    if (koinStarted) {
      runCatching { stopKoin() }
    }
    packetEventsLoader.shutdown()
    server.pluginManager.disablePlugin(this)
  }
}
