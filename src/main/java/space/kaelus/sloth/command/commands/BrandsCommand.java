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
package space.kaelus.sloth.command.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.alert.AlertManager;
import space.kaelus.sloth.command.SlothCommand;
import space.kaelus.sloth.sender.Sender;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;

public class BrandsCommand implements SlothCommand {

    @Override
    public void register(CommandManager<Sender> manager) {
        manager.command(
                manager.commandBuilder("sloth", "slothac")
                        .literal("brands")
                        .permission("sloth.brand")
                        .handler(this::execute)
        );
    }

    private void execute(CommandContext<Sender> context) {
        CommandSender nativeSender = context.sender().getNativeSender();
        AlertManager alertManager = SlothAC.getInstance().getAlertManager();

        if (nativeSender instanceof Player) {
            alertManager.toggleBrandAlerts((Player) nativeSender, false);
        } else {
            alertManager.toggleConsoleBrandAlerts();
            if (alertManager.isConsoleBrandAlertsEnabled()) {
                MessageUtil.sendMessage(nativeSender, Message.BRAND_ALERTS_ENABLED);
            } else {
                MessageUtil.sendMessage(nativeSender, Message.BRAND_ALERTS_DISABLED);
            }
        }
    }
}