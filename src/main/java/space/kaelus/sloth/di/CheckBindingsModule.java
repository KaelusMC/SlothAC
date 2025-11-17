package space.kaelus.sloth.di;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import space.kaelus.sloth.checks.CheckFactory;
import space.kaelus.sloth.checks.impl.ai.AICheck;
import space.kaelus.sloth.checks.impl.ai.ActionManager;
import space.kaelus.sloth.checks.impl.ai.DataCollectorCheck;
import space.kaelus.sloth.checks.impl.aim.AimProcessor;
import space.kaelus.sloth.checks.impl.misc.ClientBrand;

@Module
public interface CheckBindingsModule {

  @Binds
  @IntoSet
  CheckFactory bindAimProcessorFactory(AimProcessor.Factory factory);

  @Binds
  @IntoSet
  CheckFactory bindActionManagerFactory(ActionManager.Factory factory);

  @Binds
  @IntoSet
  CheckFactory bindAiCheckFactory(AICheck.Factory factory);

  @Binds
  @IntoSet
  CheckFactory bindDataCollectorCheckFactory(DataCollectorCheck.Factory factory);

  @Binds
  @IntoSet
  CheckFactory bindClientBrandFactory(ClientBrand.Factory factory);
}
