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
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.context.CommandContext;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.checks.impl.ai.AICheck;
import space.kaelus.sloth.command.CommandRegister;
import space.kaelus.sloth.command.SlothCommand;
import space.kaelus.sloth.command.requirements.PlayerSenderRequirement;
import space.kaelus.sloth.player.PlayerDataManager;
import space.kaelus.sloth.player.SlothPlayer;
import space.kaelus.sloth.sender.Sender;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;

@Singleton
public class ProbCommand implements SlothCommand, Listener {
  private final Map<UUID, ProbSession> activeSessions = new ConcurrentHashMap<>();

  private final PlayerDataManager playerDataManager;
  private final SlothAC plugin;
  private final BukkitAudiences adventure;

  @Inject
  public ProbCommand(
      PlayerDataManager playerDataManager, SlothAC plugin, BukkitAudiences adventure) {
    this.playerDataManager = playerDataManager;
    this.plugin = plugin;
    this.adventure = adventure;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
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

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    final Player player = event.getPlayer();
    final UUID uuid = player.getUniqueId();

    if (activeSessions.containsKey(uuid)) {
      stop(player);
    }

    UUID viewerUuid = null;
    for (Map.Entry<UUID, ProbSession> entry : activeSessions.entrySet()) {
      if (entry.getValue().getTargetUuid().equals(uuid)) {
        viewerUuid = entry.getKey();
        break;
      }
    }

    if (viewerUuid != null) {
      Player viewer = Bukkit.getPlayer(viewerUuid);
      if (viewer != null) {
        stop(viewer);
        MessageUtil.sendMessage(viewer, Message.PROB_DISABLED, "player", player.getName());
      } else {
        activeSessions.remove(viewerUuid);
      }
    }
  }

  private void execute(CommandContext<Sender> context) {
    final Player player = context.sender().getPlayer();
    final Player target = context.get("target");

    final ProbSession session = activeSessions.get(player.getUniqueId());

    if (session != null && session.getTargetUuid().equals(target.getUniqueId())) {
      stop(player);
      MessageUtil.sendMessage(player, Message.PROB_DISABLED, "player", target.getName());
      return;
    }

    if (session != null) {
      stop(player);
    }

    start(player, target);
    MessageUtil.sendMessage(player, Message.PROB_ENABLED, "player", target.getName());
  }

  private void start(Player viewer, Player target) {
    final UUID viewerId = viewer.getUniqueId();
    final UUID targetId = target.getUniqueId();

    final ProbSession newSession = new ProbSession(targetId);
    activeSessions.put(viewerId, newSession);

    final BukkitTask task =
        plugin
            .getServer()
            .getScheduler()
            .runTaskTimer(
                plugin,
                () -> {
                  final Player onlineViewer = Bukkit.getPlayer(viewerId);
                  final Player onlineTarget = Bukkit.getPlayer(targetId);

                  if (onlineViewer == null
                      || !onlineViewer.isOnline()
                      || onlineTarget == null
                      || !onlineTarget.isOnline()) {
                    if (onlineViewer != null) stop(onlineViewer);
                    return;
                  }

                  final SlothPlayer slothTarget = playerDataManager.getPlayer(onlineTarget);
                  if (slothTarget == null) {
                    sendActionBar(
                        onlineViewer,
                        MessageUtil.getMessage(
                            Message.PROB_NO_DATA, "player", onlineTarget.getName()));
                    return;
                  }

                  final AICheck aiCheck = slothTarget.getCheckManager().getCheck(AICheck.class);
                  if (aiCheck == null) {
                    sendActionBar(
                        onlineViewer,
                        MessageUtil.getMessage(
                            Message.PROB_NO_AICHECK, "player", onlineTarget.getName()));
                    return;
                  }

                  sendActionBar(onlineViewer, onlineTarget, aiCheck, newSession);
                },
                0L,
                2L);

    newSession.setTask(task);
  }

  private void stop(Player viewer) {
    final ProbSession session = activeSessions.remove(viewer.getUniqueId());
    if (session != null && session.getTask() != null) {
      session.getTask().cancel();
      sendActionBar(viewer, Component.empty());
    }
  }

  private void sendActionBar(Player viewer, Player target, AICheck aiCheck, ProbSession session) {
    final double probability = aiCheck.getLastProbability();
    final double buffer = aiCheck.getBuffer();
    final int ping = target.getPing();

    if (Math.abs(probability - session.getLastProbability()) < 0.0001
        && Math.abs(buffer - session.getLastBuffer()) < 0.01
        && ping == session.getLastPing()
        && session.getLastSentComponent() != null) {
      sendActionBar(viewer, session.getLastSentComponent());
      return;
    }

    Component newComponent = buildActionBar(target, probability, buffer, ping);

    session.setLastProbability(probability);
    session.setLastBuffer(buffer);
    session.setLastPing(ping);
    session.setLastSentComponent(newComponent);

    sendActionBar(viewer, newComponent);
  }

  private Component buildActionBar(Player target, double probability, double buffer, int ping) {
    final TextColor probColor = getProbColor(probability);
    final TextColor vlColor = getVlColor(buffer);
    final TextColor pingColor = getPingColor(ping);

    String bufferString = String.format(Locale.US, "%.2f", buffer);
    if (buffer > 30) {
      bufferString = "<bold>" + bufferString + "</bold>";
    }

    TagResolver resolver =
        TagResolver.builder()
            .resolver(Placeholder.unparsed("player", target.getName()))
            .resolver(TagResolver.resolver("prob_color", Tag.styling(probColor)))
            .resolver(
                Placeholder.unparsed("prob_value", String.format(Locale.US, "%.4f", probability)))
            .resolver(TagResolver.resolver("buffer_color", Tag.styling(vlColor)))
            .resolver(Placeholder.parsed("buffer_value", bufferString))
            .resolver(TagResolver.resolver("ping_color", Tag.styling(pingColor)))
            .resolver(Placeholder.unparsed("ping_value", String.valueOf(ping)))
            .build();

    return MessageUtil.getMessage(Message.PROB_FORMAT, resolver);
  }

  private void sendActionBar(Player player, Component message) {
    if (player == null || !player.isOnline()) return;
    adventure.player(player).sendActionBar(message);
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

  @Getter
  @Setter
  private static class ProbSession {
    private final UUID targetUuid;
    private BukkitTask task;

    private Component lastSentComponent;
    private double lastProbability = -1.0;
    private double lastBuffer = -1.0;
    private int lastPing = -1;

    ProbSession(UUID targetUuid) {
      this.targetUuid = targetUuid;
    }
  }
}
