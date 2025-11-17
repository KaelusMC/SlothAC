package space.kaelus.sloth.checks;

import space.kaelus.sloth.player.SlothPlayer;

public interface CheckFactory {
  ICheck create(SlothPlayer player);
}
