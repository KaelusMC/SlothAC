package space.kaelus.sloth.command.commands;

import org.bukkit.entity.Player;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import space.kaelus.sloth.command.SlothCommand;
import space.kaelus.sloth.config.LocaleManager;
import space.kaelus.sloth.player.ExemptManager;
import space.kaelus.sloth.sender.Sender;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;
import space.kaelus.sloth.utils.TimeUtil;

public class ExemptCommand implements SlothCommand {

  private final ExemptManager exemptManager;
  private final LocaleManager localeManager;

  public ExemptCommand(ExemptManager exemptManager, LocaleManager localeManager) {
    this.exemptManager = exemptManager;
    this.localeManager = localeManager;
  }

  @Override
  public void register(CommandManager<Sender> manager) {
    final var base =
        manager
            .commandBuilder("sloth", "slothac")
            .literal("exempt")
            .permission("sloth.exempt.manage");

    manager.command(
        base.required("target", PlayerParser.playerParser())
            .optional("duration", StringParser.stringParser())
            .handler(this::handleExempt));

    manager.command(
        base.literal("remove")
            .required("target", PlayerParser.playerParser())
            .handler(this::handleRemoveExempt));

    manager.command(
        base.literal("status")
            .required("target", PlayerParser.playerParser())
            .handler(this::handleStatus));
  }

  private void handleExempt(CommandContext<Sender> context) {
    final Sender sender = context.sender();
    final Player target = context.get("target");
    final String durationStr = context.getOrDefault("duration", "5m");

    long durationMillis = TimeUtil.parseDuration(durationStr);

    if (durationMillis == 0) {
      MessageUtil.sendMessage(sender.getNativeSender(), Message.EXEMPT_INVALID_DURATION);
      return;
    }

    exemptManager.addExemption(target.getUniqueId(), durationMillis);

    if (durationMillis == -1) {
      MessageUtil.sendMessage(
          sender.getNativeSender(), Message.EXEMPT_SUCCESS_PERM, "player", target.getName());
    } else {
      MessageUtil.sendMessage(
          sender.getNativeSender(),
          Message.EXEMPT_SUCCESS_TEMP,
          "player",
          target.getName(),
          "duration",
          durationStr);
    }
  }

  private void handleRemoveExempt(CommandContext<Sender> context) {
    final Sender sender = context.sender();
    final Player target = context.get("target");

    if (exemptManager.removeExemption(target.getUniqueId())) {
      MessageUtil.sendMessage(
          sender.getNativeSender(), Message.EXEMPT_REMOVE_SUCCESS, "player", target.getName());
    } else {
      MessageUtil.sendMessage(
          sender.getNativeSender(), Message.EXEMPT_REMOVE_FAIL, "player", target.getName());
    }
  }

  private void handleStatus(CommandContext<Sender> context) {
    final Sender sender = context.sender();
    final Player target = context.get("target");

    if (target.hasPermission("sloth.exempt")) {
      MessageUtil.sendMessage(
          sender.getNativeSender(),
          Message.EXEMPT_STATUS_PERM_PERMISSION,
          "player",
          target.getName());
      return;
    }

    Long expiryTime = exemptManager.getExpiryTime(target.getUniqueId());
    if (expiryTime == null) {
      MessageUtil.sendMessage(
          sender.getNativeSender(), Message.EXEMPT_STATUS_NOT_EXEMPT, "player", target.getName());
    } else if (expiryTime == -1) {
      MessageUtil.sendMessage(
          sender.getNativeSender(), Message.EXEMPT_STATUS_PERM_COMMAND, "player", target.getName());
    } else {
      long remaining = expiryTime - System.currentTimeMillis();
      if (remaining <= 0) {
        MessageUtil.sendMessage(
            sender.getNativeSender(), Message.EXEMPT_STATUS_EXPIRED, "player", target.getName());
        exemptManager.removeExemption(target.getUniqueId());
      } else {
        String remainingStr = TimeUtil.formatDuration(remaining, this.localeManager);
        MessageUtil.sendMessage(
            sender.getNativeSender(),
            Message.EXEMPT_STATUS_TEMP,
            "player",
            target.getName(),
            "duration",
            remainingStr);
      }
    }
  }
}
