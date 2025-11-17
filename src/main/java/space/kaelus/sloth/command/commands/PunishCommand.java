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
package space.kaelus.sloth.command.commands;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.bukkit.OfflinePlayer;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.OfflinePlayerParser;
import org.incendo.cloud.context.CommandContext;
import space.kaelus.sloth.command.SlothCommand;
import space.kaelus.sloth.database.DatabaseManager;
import space.kaelus.sloth.sender.Sender;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;

@Singleton
public class PunishCommand implements SlothCommand {

  private final DatabaseManager databaseManager;

  @Inject
  public PunishCommand(DatabaseManager databaseManager) {
    this.databaseManager = databaseManager;
  }

  @Override
  public void register(CommandManager<Sender> manager) {
    final var baseBuilder =
        manager
            .commandBuilder("sloth", "slothac", "slothac")
            .literal("punish")
            .permission("sloth.punish.manage");

    manager.command(
        baseBuilder
            .literal("reset")
            .required("target", OfflinePlayerParser.offlinePlayerParser())
            .handler(this::reset));
  }

  private void reset(CommandContext<Sender> context) {
    final Sender sender = context.sender();
    final OfflinePlayer target = context.get("target");

    databaseManager.getDatabase().resetAllViolationLevels(target.getUniqueId());

    MessageUtil.sendMessage(
        sender.getNativeSender(), Message.PUNISH_RESET_SUCCESS, "player", target.getName());
  }
}
