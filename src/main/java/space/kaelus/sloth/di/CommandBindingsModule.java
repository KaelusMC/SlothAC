package space.kaelus.sloth.di;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import space.kaelus.sloth.command.SlothCommand;
import space.kaelus.sloth.command.commands.AlertsCommand;
import space.kaelus.sloth.command.commands.BrandsCommand;
import space.kaelus.sloth.command.commands.DataCollectCommand;
import space.kaelus.sloth.command.commands.ExemptCommand;
import space.kaelus.sloth.command.commands.HelpCommand;
import space.kaelus.sloth.command.commands.HistoryCommand;
import space.kaelus.sloth.command.commands.LogsCommand;
import space.kaelus.sloth.command.commands.ProbCommand;
import space.kaelus.sloth.command.commands.ProfileCommand;
import space.kaelus.sloth.command.commands.PunishCommand;
import space.kaelus.sloth.command.commands.ReloadCommand;
import space.kaelus.sloth.command.commands.StatsCommand;
import space.kaelus.sloth.command.commands.SuspiciousCommand;

@Module
public interface CommandBindingsModule {
  @Binds
  @IntoSet
  SlothCommand bindAlertsCommand(AlertsCommand command);

  @Binds
  @IntoSet
  SlothCommand bindBrandsCommand(BrandsCommand command);

  @Binds
  @IntoSet
  SlothCommand bindDataCollectCommand(DataCollectCommand command);

  @Binds
  @IntoSet
  SlothCommand bindExemptCommand(ExemptCommand command);

  @Binds
  @IntoSet
  SlothCommand bindHelpCommand(HelpCommand command);

  @Binds
  @IntoSet
  SlothCommand bindHistoryCommand(HistoryCommand command);

  @Binds
  @IntoSet
  SlothCommand bindLogsCommand(LogsCommand command);

  @Binds
  @IntoSet
  SlothCommand bindProfileCommand(ProfileCommand command);

  @Binds
  @IntoSet
  SlothCommand bindProbCommand(ProbCommand command);

  @Binds
  @IntoSet
  SlothCommand bindPunishCommand(PunishCommand command);

  @Binds
  @IntoSet
  SlothCommand bindReloadCommand(ReloadCommand command);

  @Binds
  @IntoSet
  SlothCommand bindStatsCommand(StatsCommand command);

  @Binds
  @IntoSet
  SlothCommand bindSuspiciousCommand(SuspiciousCommand command);
}
