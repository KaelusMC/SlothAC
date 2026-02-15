package space.kaelus.sloth.api.service

import java.util.UUID
import java.util.concurrent.CompletableFuture
import org.bukkit.entity.Player

/** Access to punishment/violation levels. */
interface PunishmentApi {
  /**
   * Asynchronously fetches current violation level for a group.
   *
   * @param playerId player UUID
   * @param groupName punish group name
   * @return future with current VL (0 if missing)
   */
  fun getViolationLevel(playerId: UUID, groupName: String): CompletableFuture<Int>

  /**
   * Asynchronously resets violation level for a group.
   *
   * @param playerId player UUID
   * @param groupName punish group name
   * @return completion future
   */
  fun resetViolationLevel(playerId: UUID, groupName: String): CompletableFuture<Void>

  /** Convenience overload for Bukkit [Player]. */
  fun getViolationLevel(player: Player?, groupName: String): CompletableFuture<Int> {
    if (player == null) {
      return CompletableFuture.completedFuture(0)
    }
    return getViolationLevel(player.uniqueId, groupName)
  }

  fun resetViolationLevel(player: Player?, groupName: String): CompletableFuture<Void> {
    if (player == null) {
      return CompletableFuture.completedFuture(null)
    }
    return resetViolationLevel(player.uniqueId, groupName)
  }
}
