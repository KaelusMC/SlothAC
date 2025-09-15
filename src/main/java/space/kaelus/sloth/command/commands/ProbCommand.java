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

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import space.kaelus.sloth.config.LocaleManager;
import space.kaelus.sloth.player.PlayerDataManager;
import space.kaelus.sloth.player.SlothPlayer;
import space.kaelus.sloth.sender.Sender;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;

public class ProbCommand implements SlothCommand {
  private static final Map<UUID, UUID> probsMap = new ConcurrentHashMap<>();
  private static final Map<UUID, BukkitTask> probTasks = new ConcurrentHashMap<>();
  private static final Map<UUID, ProbState> probStates = new ConcurrentHashMap<>();

  private final PlayerDataManager playerDataManager;
  private final LocaleManager localeManager;
  private final SlothAC plugin;

  public ProbCommand(
      PlayerDataManager playerDataManager, LocaleManager localeManager, SlothAC plugin) {
    this.playerDataManager = playerDataManager;
    this.localeManager = localeManager;
    this.plugin = plugin;
  }

  @Override
  public void register(CommandManager<Sender> manager) {
    manager.command(
        manager
            .commandBuilder("sloth", "slothac")
            .literal("prob")
            .permission("sloth.prob")
            .required("target", PlayerParser.playerParser())
            .apply(
                CommandRegister.REQUIREMENT_FACTORY.create(
                    PlayerSenderRequirement.PLAYER_SENDER_REQUIREMENT))
            .handler(this::execute));
  }

  private void execute(CommandContext<Sender> context) {
    final Player player = context.sender().getPlayer();
    final Player target = context.get("target");

    if (probsMap.containsKey(player.getUniqueId())
        && probsMap.get(player.getUniqueId()).equals(target.getUniqueId())) {
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

    ProbState state = new ProbState(this.localeManager);
    probStates.put(player.getUniqueId(), state);

    BukkitTask task =
        plugin
            .getServer()
            .getScheduler()
            .runTaskTimer(
                plugin,
                () -> {
                  if (!player.isOnline() || !target.isOnline()) {
                    stop(player);
                    return;
                  }

                  final ProbState currentState = probStates.get(player.getUniqueId());
                  if (currentState == null) {
                    stop(player);
                    return;
                  }

                  final SlothPlayer slothTarget = playerDataManager.getPlayer(target);
                  if (slothTarget == null) {
                    sendActionBar(
                        player,
                        MessageUtil.getMessage(Message.PROB_NO_DATA, "player", target.getName()));
                    return;
                  }

                  final AICheck aiCheck = slothTarget.getCheckManager().getCheck(AICheck.class);
                  if (aiCheck == null) {
                    sendActionBar(
                        player,
                        MessageUtil.getMessage(
                            Message.PROB_NO_AICHECK, "player", target.getName()));
                    return;
                  }

                  final double probability = aiCheck.getLastProbability();
                  final double violationLevel = aiCheck.getBuffer();
                  final int ping = target.getPing();

                  final TextColor probColor = getProbColor(probability);
                  final TextColor vlColor = getVlColor(violationLevel);
                  final TextColor pingColor = getPingColor(ping);

                  TextComponent bufferComponent =
                      Component.text(String.format(Locale.US, "%.2f", violationLevel), vlColor);
                  if (violationLevel > 30) {
                    bufferComponent = bufferComponent.decorate(TextDecoration.BOLD);
                  }

                  Component actionBarMessage =
                      Component.text()
                          .append(currentState.labelProb().color(probColor))
                          .append(currentState.openParen().color(probColor))
                          .append(Component.text(target.getName(), probColor))
                          .append(currentState.closeParen().color(probColor))
                          .append(
                              Component.text(
                                  String.format(Locale.US, "%.4f", probability), probColor))
                          .append(currentState.separator())
                          .append(currentState.labelBuffer().color(vlColor))
                          .append(currentState.colon().color(vlColor))
                          .append(bufferComponent)
                          .append(currentState.separator())
                          .append(currentState.labelPing().color(pingColor))
                          .append(currentState.colon().color(pingColor))
                          .append(Component.text(ping, pingColor))
                          .append(currentState.suffixPing().color(pingColor))
                          .build();

                  sendActionBar(player, actionBarMessage);
                },
                0L,
                2L);

    probTasks.put(player.getUniqueId(), task);
  }

  private void stop(Player player) {
    probsMap.remove(player.getUniqueId());
    probStates.remove(player.getUniqueId());
    BukkitTask task = probTasks.remove(player.getUniqueId());
    if (task != null) {
      task.cancel();
    }
    sendActionBar(player, Component.empty());
  }

  private void sendActionBar(Player player, Component message) {
    if (player == null || !player.isOnline()) return;
    plugin.getAdventure().player(player).sendActionBar(message);
  }

  private TextColor getProbColor(double probability) {
    if (probability > 0.9) return NamedTextColor.RED;
    if (probability > 0.5) return NamedTextColor.YELLOW;
    return NamedTextColor.GREEN;
  }

  private TextColor getVlColor(double violationLevel) {
    if (violationLevel > 30) return NamedTextColor.DARK_RED;
    if (violationLevel > 15) return NamedTextColor.RED;
    return NamedTextColor.GREEN;
  }

  private TextColor getPingColor(int ping) {
    if (ping > 150) return NamedTextColor.RED;
    if (ping > 80) return NamedTextColor.YELLOW;
    return NamedTextColor.GREEN;
  }

  private record ProbState(
      Component labelProb,
      Component labelBuffer,
      Component labelPing,
      Component separator,
      Component suffixPing,
      Component openParen,
      Component closeParen,
      Component colon) {
    ProbState(LocaleManager lm) {
      this(
          Component.text(lm.getRawMessage(Message.PROB_FORMAT_LABEL_PROB)),
          Component.text(lm.getRawMessage(Message.PROB_FORMAT_LABEL_BUFFER)),
          Component.text(lm.getRawMessage(Message.PROB_FORMAT_LABEL_PING)),
          Component.text(lm.getRawMessage(Message.PROB_FORMAT_SEPARATOR), NamedTextColor.DARK_GRAY),
          Component.text(lm.getRawMessage(Message.PROB_FORMAT_SUFFIX_PING)),
          Component.text(" ("),
          Component.text("): "),
          Component.text(": "));
    }
  }
}
