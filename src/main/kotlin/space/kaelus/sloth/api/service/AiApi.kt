package space.kaelus.sloth.api.service

import java.util.Optional
import java.util.UUID
import org.bukkit.entity.Player
import space.kaelus.sloth.api.model.AiSnapshot

/** Access to AI status and the latest prediction snapshot per player. */
interface AiApi {
  /** Returns true when AI integration is enabled. */
  fun isEnabled(): Boolean

  /**
   * Latest AI snapshot for a player, if available.
   *
   * @param playerId player UUID
   * @return snapshot with probability/buffer/dmg/prob90
   */
  fun getSnapshot(playerId: UUID): Optional<AiSnapshot>

  /** Convenience overload for Bukkit [Player]. */
  fun getSnapshot(player: Player?): Optional<AiSnapshot> {
    if (player == null) {
      return Optional.empty()
    }
    return getSnapshot(player.uniqueId)
  }
}
