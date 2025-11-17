package space.kaelus.sloth.di;

import dagger.Module;
import dagger.Provides;
import java.util.logging.Logger;
import javax.inject.Singleton;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import space.kaelus.sloth.SlothAC;

@Module
public class SlothModule {

  @Provides
  @Singleton
  static BukkitAudiences provideBukkitAudiences(SlothAC plugin) {
    return BukkitAudiences.create(plugin);
  }

  @Provides
  @Singleton
  static Logger provideLogger(SlothAC plugin) {
    return plugin.getLogger();
  }
}
