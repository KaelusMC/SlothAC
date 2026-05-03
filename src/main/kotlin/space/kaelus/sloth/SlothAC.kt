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
import space.kaelus.sloth.integration.SlothFlags

class SlothAC : JavaPlugin() {
  private var core: SlothCore? = null
  private val packetEventsLoader = PacketEventsLoader(this)
  private var packetEventsLoadFailure: Throwable? = null
  private var runtimeStopped = false

  override fun onLoad() {
    packetEventsLoadFailure = runCatching { packetEventsLoader.load() }.exceptionOrNull()
    runCatching { SlothFlags.register(logger) }
      .onFailure { logger.log(Level.WARNING, "Failed to register WorldGuard flags", it) }
  }

  override fun onEnable() {
    runtimeStopped = false
    packetEventsLoadFailure?.let { failure ->
      handleEnableFailure(failure)
      return
    }

    runCatching(::enableRuntime).onFailure(::handleEnableFailure)
  }

  override fun onDisable() {
    shutdownRuntime()
  }

  fun onReload() {
    core?.reload()
  }

  private companion object {
    const val BSTATS_PLUGIN_ID = 30367
  }

  private fun enableRuntime() {
    val koinApp = startKoin { modules(slothModules(this@SlothAC)) }
    core = koinApp.koin.get()
    core?.enable()
    Metrics(this, BSTATS_PLUGIN_ID)
  }

  private fun handleEnableFailure(failure: Throwable) {
    logger.log(Level.SEVERE, "Sloth failed to start and will disable itself safely.", failure)
    shutdownRuntime()
    server.pluginManager.disablePlugin(this)
  }

  private fun shutdownRuntime() {
    if (runtimeStopped) {
      return
    }
    runtimeStopped = true

    runCatching { core?.disable() }
    core = null
    runCatching { stopKoin() }
    packetEventsLoader.shutdown()
  }
}
