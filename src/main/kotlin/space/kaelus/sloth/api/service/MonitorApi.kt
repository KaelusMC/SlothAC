package space.kaelus.sloth.api.service

import java.util.Optional
import java.util.UUID
import org.bukkit.entity.Player
import space.kaelus.sloth.api.model.MonitorSnapshot

/** Access to current monitor data for a player. */
interface MonitorApi {
  /** Returns the latest monitor snapshot if available. */
  fun getSnapshot(playerId: UUID): Optional<MonitorSnapshot>

  /** Convenience overload for Bukkit [Player]. */
  fun getSnapshot(player: Player?): Optional<MonitorSnapshot> {
    if (player == null) {
      return Optional.empty()
    }
    return getSnapshot(player.uniqueId)
  }
}
