package space.kaelus.sloth.checks

import space.kaelus.sloth.player.SlothPlayer

fun interface CheckFactory {
  fun create(player: SlothPlayer): ICheck
}
