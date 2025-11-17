package space.kaelus.sloth.player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.bukkit.entity.Player;

@Singleton
public class ExemptManager {
  private final Map<UUID, Long> temporaryExemptions = new ConcurrentHashMap<>();

  @Inject
  public ExemptManager() {}

  public boolean isExempt(Player player) {
    if (player == null) {
      return false;
    }

    if (player.hasPermission("sloth.exempt")) {
      return true;
    }

    Long expiryTime = temporaryExemptions.get(player.getUniqueId());
    if (expiryTime == null) {
      return false;
    }

    if (expiryTime != -1 && System.currentTimeMillis() > expiryTime) {
      temporaryExemptions.remove(player.getUniqueId());
      return false;
    }

    return true;
  }

  public void addExemption(UUID uuid, long durationMillis) {
    if (durationMillis == -1) {
      temporaryExemptions.put(uuid, -1L);
    } else {
      long expiryTime = System.currentTimeMillis() + durationMillis;
      temporaryExemptions.put(uuid, expiryTime);
    }
  }

  public boolean removeExemption(UUID uuid) {
    return temporaryExemptions.remove(uuid) != null;
  }

  public Long getExpiryTime(UUID uuid) {
    return temporaryExemptions.get(uuid);
  }
}
