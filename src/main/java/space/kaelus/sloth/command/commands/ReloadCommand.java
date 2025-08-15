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

import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.command.SlothCommand;
import space.kaelus.sloth.player.SlothPlayer;
import space.kaelus.sloth.sender.Sender;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;

public class ReloadCommand implements SlothCommand {

    @Override
    public void register(CommandManager<Sender> manager) {
        manager.command(
                manager.commandBuilder("sloth")
                        .literal("reload")
                        .permission("sloth.reload")
                        .handler(this::execute)
        );
    }

    private void execute(CommandContext<Sender> context) {
        SlothAC plugin = SlothAC.getInstance();

        MessageUtil.sendMessage(context.sender().getNativeSender(), Message.RELOAD_START);

        plugin.getConfigManager().reloadConfig();
        plugin.loadAI();

        if (plugin.getPlayerDataManager() != null) {
            for (SlothPlayer onlinePlayer : plugin.getPlayerDataManager().getPlayers()) {
                User user = onlinePlayer.getUser();
                if (user != null) {
                    ChannelHelper.runInEventLoop(user.getChannel(), onlinePlayer::reload);
                }
            }
        }

        MessageUtil.sendMessage(context.sender().getNativeSender(), Message.RELOAD_SUCCESS);
    }
}