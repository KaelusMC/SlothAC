package space.kaelus.sloth.di;

import dagger.BindsInstance;
import dagger.Component;
import javax.inject.Singleton;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.SlothCore;

@Singleton
@Component(modules = {SlothModule.class, CommandBindingsModule.class, CheckBindingsModule.class})
public interface SlothComponent {

  SlothCore core();

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder plugin(SlothAC plugin);

    SlothComponent build();
  }
}
