package space.kaelus.sloth.command.commands;

import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.checks.impl.ai.AICheck;
import space.kaelus.sloth.command.SlothCommand;
import space.kaelus.sloth.database.DatabaseManager;
import space.kaelus.sloth.database.ViolationDatabase;
import space.kaelus.sloth.player.PlayerDataManager;
import space.kaelus.sloth.sender.Sender;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;

public class StatsCommand implements SlothCommand {

  private final SlothAC plugin;
  private final DatabaseManager databaseManager;
  private final PlayerDataManager playerDataManager;

  public StatsCommand(
      SlothAC plugin, DatabaseManager databaseManager, PlayerDataManager playerDataManager) {
    this.plugin = plugin;
    this.databaseManager = databaseManager;
    this.playerDataManager = playerDataManager;
  }

  @Override
  public void register(CommandManager<Sender> manager) {
    manager.command(
        manager
            .commandBuilder("sloth", "slothac")
            .literal("stats")
            .permission("sloth.stats")
            .handler(this::execute));
  }

  private void execute(CommandContext<Sender> context) {
    final Sender sender = context.sender();
    final ViolationDatabase db = databaseManager.getDatabase();

    if (db == null) {
      sender.sendMessage(MessageUtil.getMessage(Message.HISTORY_DISABLED));
      return;
    }

    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              long since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);

              int totalFlags = db.getLogCount(since);
              int uniqueViolators = db.getUniqueViolatorsSince(since);

              Bukkit.getScheduler()
                  .runTask(
                      plugin,
                      () -> {
                        MessageUtil.sendMessageList(
                            sender.getNativeSender(),
                            Message.STATS_LINES,
                            "flags_24h",
                            String.valueOf(totalFlags),
                            "violators_24h",
                            String.valueOf(uniqueViolators),
                            "online_players",
                            String.valueOf(Bukkit.getOnlinePlayers().size()),
                            "suspicious_now",
                            String.valueOf(getSuspiciousCount()));
                      });
            });
  }

  private long getSuspiciousCount() {
    return playerDataManager.getPlayers().stream()
        .filter(
            sp -> {
              AICheck check = sp.getCheckManager().getCheck(AICheck.class);
              return check != null && check.getBuffer() > 10;
            })
        .count();
  }
}
