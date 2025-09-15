/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2025 KaelusMC
 *
 * This file contains code derived from GrimAC.
 * The original authors of GrimAC are credited below.
 *
 * Copyright (c) 2021-2025 GrimAC, DefineOutside and contributors.
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
package space.kaelus.sloth.command;

import io.leangen.geantyref.TypeToken;
import java.util.function.Function;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.exception.InvalidSyntaxException;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.processors.requirements.RequirementApplicable;
import org.incendo.cloud.processors.requirements.RequirementPostprocessor;
import org.incendo.cloud.processors.requirements.Requirements;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.alert.AlertManager;
import space.kaelus.sloth.checks.impl.ai.DataCollectorManager;
import space.kaelus.sloth.command.commands.*;
import space.kaelus.sloth.command.handler.SlothCommandFailureHandler;
import space.kaelus.sloth.config.ConfigManager;
import space.kaelus.sloth.config.LocaleManager;
import space.kaelus.sloth.database.DatabaseManager;
import space.kaelus.sloth.player.PlayerDataManager;
import space.kaelus.sloth.sender.Sender;
import space.kaelus.sloth.utils.MessageUtil;

public class CommandRegister {

  public static final CloudKey<Requirements<Sender, SenderRequirement>> REQUIREMENT_KEY =
      CloudKey.of("sloth_requirements", new TypeToken<>() {});

  public static final RequirementApplicable.RequirementApplicableFactory<Sender, SenderRequirement>
      REQUIREMENT_FACTORY = RequirementApplicable.factory(REQUIREMENT_KEY);

  private static boolean commandsRegistered = false;

  public static void registerCommands(
      org.incendo.cloud.CommandManager<Sender> commandManager,
      SlothAC plugin,
      AlertManager alertManager,
      DataCollectorManager dataCollectorManager,
      DatabaseManager databaseManager,
      ConfigManager configManager,
      LocaleManager localeManager,
      PlayerDataManager playerDataManager) {

    if (commandsRegistered) return;

    new HelpCommand().register(commandManager);
    new AlertsCommand(alertManager).register(commandManager);
    new ReloadCommand(plugin).register(commandManager);
    new ProbCommand(playerDataManager, localeManager, plugin).register(commandManager);
    new DataCollectCommand(dataCollectorManager).register(commandManager);
    new ProfileCommand(playerDataManager, localeManager).register(commandManager);
    new HistoryCommand(plugin, databaseManager, configManager, localeManager)
        .register(commandManager);
    new LogsCommand(plugin, databaseManager, configManager, localeManager).register(commandManager);
    new PunishCommand(databaseManager).register(commandManager);
    new BrandsCommand(alertManager).register(commandManager);
    new SuspiciousCommand(playerDataManager, alertManager).register(commandManager);
    new StatsCommand(plugin, databaseManager, playerDataManager).register(commandManager);

    final RequirementPostprocessor<Sender, SenderRequirement> senderRequirementPostprocessor =
        RequirementPostprocessor.of(REQUIREMENT_KEY, new SlothCommandFailureHandler());
    commandManager.registerCommandPostProcessor(senderRequirementPostprocessor);

    registerExceptionHandler(
        commandManager, InvalidSyntaxException.class, e -> MessageUtil.format(e.correctSyntax()));

    commandsRegistered = true;
  }

  private static <E extends Exception> void registerExceptionHandler(
      org.incendo.cloud.CommandManager<Sender> commandManager,
      Class<E> ex,
      Function<E, ComponentLike> toComponent) {
    commandManager
        .exceptionController()
        .registerHandler(
            ex,
            (c) ->
                c.context()
                    .sender()
                    .sendMessage(
                        toComponent
                            .apply(c.exception())
                            .asComponent()
                            .colorIfAbsent(NamedTextColor.RED)));
  }
}
