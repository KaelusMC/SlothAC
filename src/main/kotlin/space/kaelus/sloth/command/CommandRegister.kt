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

import io.leangen.geantyref.TypeToken
import java.util.function.Function
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.NamedTextColor
import org.incendo.cloud.exception.InvalidSyntaxException
import org.incendo.cloud.key.CloudKey
import org.incendo.cloud.processors.requirements.RequirementApplicable
import org.incendo.cloud.processors.requirements.RequirementPostprocessor
import org.incendo.cloud.processors.requirements.Requirements
import space.kaelus.sloth.command.handler.SlothCommandFailureHandler
import space.kaelus.sloth.sender.Sender
import space.kaelus.sloth.utils.MessageUtil

class CommandRegister(
  private val commands: Collection<SlothCommand>,
  private val failureHandler: SlothCommandFailureHandler,
) {
  private var commandsRegistered = false

  fun registerCommands(commandManager: org.incendo.cloud.CommandManager<Sender>) {
    if (commandsRegistered) {
      return
    }

    for (command in commands) {
      command.register(commandManager)
    }

    val senderRequirementPostprocessor =
      RequirementPostprocessor.of(REQUIREMENT_KEY, failureHandler)
    commandManager.registerCommandPostProcessor(senderRequirementPostprocessor)

    registerExceptionHandler(commandManager, InvalidSyntaxException::class.java) { e ->
      MessageUtil.format(e.correctSyntax())
    }

    commandsRegistered = true
  }

  private fun <E : Exception> registerExceptionHandler(
    commandManager: org.incendo.cloud.CommandManager<Sender>,
    ex: Class<E>,
    toComponent: Function<E, ComponentLike>,
  ) {
    commandManager.exceptionController().registerHandler(ex) { c ->
      c.context()
        .sender()
        .sendMessage(
          toComponent.apply(c.exception()).asComponent().colorIfAbsent(NamedTextColor.RED)
        )
    }
  }

  companion object {
    @JvmField
    val REQUIREMENT_KEY: CloudKey<Requirements<Sender, SenderRequirement>> =
      CloudKey.of(
        "sloth_requirements",
        object : TypeToken<Requirements<Sender, SenderRequirement>>() {},
      )

    @JvmField
    val REQUIREMENT_FACTORY:
      RequirementApplicable.RequirementApplicableFactory<Sender, SenderRequirement> =
      RequirementApplicable.factory(REQUIREMENT_KEY)
  }
}
