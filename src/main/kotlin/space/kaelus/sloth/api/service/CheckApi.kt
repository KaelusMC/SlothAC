package space.kaelus.sloth.api.service

import java.util.Optional
import java.util.UUID
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.bukkit.entity.Player
import space.kaelus.sloth.api.model.CheckInfo

/** Access to per-player check list and metadata. */
interface CheckApi {
  /** Returns all checks created for a player. */
  fun listChecks(playerId: UUID): ImmutableList<CheckInfo>

  /** Returns a check by name or configName for a player. */
  fun getCheck(playerId: UUID, checkName: String): Optional<CheckInfo>

  /** Convenience overload for Bukkit [Player]. */
  fun listChecks(player: Player?): ImmutableList<CheckInfo> {
    if (player == null) {
      return persistentListOf()
    }
    return listChecks(player.uniqueId)
  }

  fun getCheck(player: Player?, checkName: String): Optional<CheckInfo> {
    if (player == null) {
      return Optional.empty()
    }
    return getCheck(player.uniqueId, checkName)
  }
}
