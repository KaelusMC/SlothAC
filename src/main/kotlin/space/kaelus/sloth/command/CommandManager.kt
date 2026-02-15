/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2026 KaelusMC
 *
 * This file contains code derived from GrimAC.
 * The original authors of GrimAC are credited below.
 *
 * Copyright (c) 2021-2026 GrimAC, DefineOutside and contributors.
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
package space.kaelus.sloth.command

import org.bukkit.command.CommandSender
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.bukkit.CloudBukkitCapabilities
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.LegacyPaperCommandManager
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.sender.Sender

class CommandManager(
  private val plugin: SlothAC,
  private val commandRegister: CommandRegister,
  private val senderMapper: SenderMapper<CommandSender, Sender>,
) {
  private var cloudManager: LegacyPaperCommandManager<Sender>? = null

  fun registerCommands() {
    if (cloudManager != null) {
      return
    }

    val manager = setupCloud(plugin) ?: return

    commandRegister.registerCommands(manager)
    cloudManager = manager
  }

  private fun setupCloud(plugin: SlothAC): LegacyPaperCommandManager<Sender>? {
    val manager =
      try {
        LegacyPaperCommandManager(plugin, ExecutionCoordinator.simpleCoordinator(), senderMapper)
      } catch (e: Exception) {
        plugin.logger.severe("Failed to initialize Cloud Command Manager: ${e.message}")
        e.printStackTrace()
        return null
      }

    if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
      manager.registerBrigadier()
    } else if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
      manager.registerAsynchronousCompletions()
    }

    return manager
  }
}
