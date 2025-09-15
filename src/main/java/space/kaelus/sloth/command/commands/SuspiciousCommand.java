package space.kaelus.sloth.command.commands;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.DoubleParser;
import space.kaelus.sloth.alert.AlertManager;
import space.kaelus.sloth.checks.impl.ai.AICheck;
import space.kaelus.sloth.command.CommandRegister;
import space.kaelus.sloth.command.SlothCommand;
import space.kaelus.sloth.command.requirements.PlayerSenderRequirement;
import space.kaelus.sloth.player.PlayerDataManager;
import space.kaelus.sloth.player.SlothPlayer;
import space.kaelus.sloth.sender.Sender;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;

public class SuspiciousCommand implements SlothCommand {
  private final PlayerDataManager playerDataManager;
  private final AlertManager alertManager;

  public SuspiciousCommand(PlayerDataManager playerDataManager, AlertManager alertManager) {
    this.playerDataManager = playerDataManager;
    this.alertManager = alertManager;
  }

  @Override
  public void register(CommandManager<Sender> manager) {
    final var base =
        manager
            .commandBuilder("sloth", "slothac")
            .literal("suspicious")
            .permission("sloth.suspicious");

    manager.command(
        base.literal("alerts")
            .permission("sloth.suspicious.alerts")
            .apply(
                CommandRegister.REQUIREMENT_FACTORY.create(
                    PlayerSenderRequirement.PLAYER_SENDER_REQUIREMENT))
            .handler(this::executeAlerts));

    manager.command(
        base.literal("list")
            .permission("sloth.suspicious.list")
            .flag(manager.flagBuilder("buffer").withComponent(DoubleParser.doubleParser(0.0)))
            .handler(this::executeList));

    manager.command(
        base.literal("top").permission("sloth.suspicious.top").handler(this::executeTop));
  }

  private void executeAlerts(CommandContext<Sender> context) {
    final Player player = context.sender().getPlayer();
    alertManager.toggleSuspiciousAlerts(player);
  }

  private void executeList(CommandContext<Sender> context) {
    final Sender sender = context.sender();

    final Double bufferFlag = context.flags().get("buffer");
    final double bufferFilter = bufferFlag != null ? bufferFlag : 0.0;

    List<SlothPlayer> suspiciousPlayers =
        playerDataManager.getPlayers().stream()
            .filter(
                sp -> {
                  AICheck check = sp.getCheckManager().getCheck(AICheck.class);
                  return check != null && check.getBuffer() > bufferFilter;
                })
            .sorted(
                Comparator.comparingDouble(
                    sp -> -sp.getCheckManager().getCheck(AICheck.class).getBuffer()))
            .collect(Collectors.toList());

    if (suspiciousPlayers.isEmpty()) {
      sender.sendMessage(MessageUtil.getMessage(Message.SUSPICIOUS_LIST_EMPTY));
      return;
    }

    sender.sendMessage(
        MessageUtil.getMessage(
            Message.SUSPICIOUS_LIST_HEADER, "count", String.valueOf(suspiciousPlayers.size())));
    for (SlothPlayer sp : suspiciousPlayers) {
      AICheck aiCheck = sp.getCheckManager().getCheck(AICheck.class);
      double buffer = aiCheck.getBuffer();
      String playerName = sp.getPlayer().getName();

      Component entry =
          MessageUtil.getMessage(
              Message.SUSPICIOUS_LIST_ENTRY,
              "player",
              playerName,
              "buffer",
              String.format("%.1f", buffer),
              "ping",
              String.valueOf(sp.getPlayer().getPing()));

      sender.sendMessage(entry);
    }
  }

  private void executeTop(CommandContext<Sender> context) {
    final Sender sender = context.sender();

    SlothPlayer topPlayer =
        playerDataManager.getPlayers().stream()
            .filter(sp -> sp.getCheckManager().getCheck(AICheck.class) != null)
            .max(
                Comparator.comparingDouble(
                    sp -> sp.getCheckManager().getCheck(AICheck.class).getBuffer()))
            .orElse(null);

    if (topPlayer == null || topPlayer.getCheckManager().getCheck(AICheck.class).getBuffer() == 0) {
      sender.sendMessage(MessageUtil.getMessage(Message.SUSPICIOUS_TOP_NONE));
      return;
    }

    String playerName = topPlayer.getPlayer().getName();
    double buffer = topPlayer.getCheckManager().getCheck(AICheck.class).getBuffer();

    Component message =
        MessageUtil.getMessage(
            Message.SUSPICIOUS_TOP_PLAYER,
            "player",
            playerName,
            "buffer",
            String.format("%.1f", buffer));

    sender.sendMessage(message);
  }
}
