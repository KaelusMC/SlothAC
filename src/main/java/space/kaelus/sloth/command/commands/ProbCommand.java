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

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.context.CommandContext;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.checks.impl.ai.AICheck;
import space.kaelus.sloth.command.CommandRegister;
import space.kaelus.sloth.command.SlothCommand;
import space.kaelus.sloth.command.requirements.PlayerSenderRequirement;
import space.kaelus.sloth.player.SlothPlayer;
import space.kaelus.sloth.sender.Sender;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProbCommand implements SlothCommand {
    private static final Map<UUID, UUID> probsMap = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitTask> probTasks = new ConcurrentHashMap<>();

    @Override
    public void register(CommandManager<Sender> manager) {
        manager.command(
                manager.commandBuilder("sloth", "slothac")
                        .literal("prob")
                        .permission("sloth.prob")
                        .required("target", PlayerParser.playerParser())
                        .apply(CommandRegister.REQUIREMENT_FACTORY.create(PlayerSenderRequirement.PLAYER_SENDER_REQUIREMENT))
                        .handler(this::execute)
        );
    }

    private void execute(CommandContext<Sender> context) {
        final Player player = context.sender().getPlayer();
        final Player target = context.get("target");

        if (probsMap.containsKey(player.getUniqueId()) && probsMap.get(player.getUniqueId()).equals(target.getUniqueId())) {
            stop(player);
            MessageUtil.sendMessage(player, Message.PROB_DISABLED, "player", target.getName());
            return;
        }

        if (probTasks.containsKey(player.getUniqueId())) {
            stop(player);
        }

        start(player, target);
        MessageUtil.sendMessage(player, Message.PROB_ENABLED, "player", target.getName());
    }

    private void start(Player player, Player target) {
        probsMap.put(player.getUniqueId(), target.getUniqueId());

        BukkitTask task = SlothAC.getInstance().getServer().getScheduler().runTaskTimer(SlothAC.getInstance(), () -> {
            if (!player.isOnline() || !target.isOnline()) {
                stop(player);
                return;
            }

            SlothPlayer slothTarget = SlothAC.getInstance().getPlayerDataManager().getPlayer(target);
            if (slothTarget == null) {
                sendActionBar(player, MessageUtil.getMessage(Message.PROB_NO_DATA, "player", target.getName()));
                return;
            }

            AICheck aiCheck = slothTarget.getCheckManager().getCheck(AICheck.class);
            if (aiCheck == null) {
                sendActionBar(player, MessageUtil.getMessage(Message.PROB_NO_AICHECK, "player", target.getName()));
                return;
            }

            double probability = aiCheck.getLastProbability();
            double violationLevel = aiCheck.getBuffer();
            int ping = target.getPing();

            String probColor = probability > 0.9 ? "&c" : probability > 0.5 ? "&e" : "&a";
            String vlColor = violationLevel > 30 ? "&4&l" : violationLevel > 15 ? "&c" : violationLevel > 5 ? "&e" : "&a";
            String pingColor = ping > 150 ? "&c" : ping > 80 ? "&e" : "&a";

            String format = SlothAC.getInstance().getLocaleManager().getRawMessage(Message.PROB_ACTIONBAR_FORMAT);

            String placeholders = format
                    .replace("%prob_color%", probColor)
                    .replace("%vl_color%", vlColor)
                    .replace("%ping_color%", pingColor)
                    .replace("%player%", target.getName())
                    .replace("%ping%", String.valueOf(ping));

            String message = String.format(Locale.US, placeholders, probability, violationLevel);

            sendActionBar(player, MessageUtil.format(message));

        }, 0L, 2L);

        probTasks.put(player.getUniqueId(), task);
    }

    private void stop(Player player) {
        probsMap.remove(player.getUniqueId());
        BukkitTask task = probTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        sendActionBar(player, Component.empty());
    }

    private void sendActionBar(Player player, Component message) {
        if (player == null || !player.isOnline()) return;
        SlothAC.getInstance().getAdventure().player(player).sendActionBar(message);
    }
}