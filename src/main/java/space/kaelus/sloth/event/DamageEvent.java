/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2025 KaelusMC
 *
 * SlothAC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SlothAC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package space.kaelus.sloth.event;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import space.kaelus.sloth.player.PlayerDataManager;
import space.kaelus.sloth.player.SlothPlayer;

@Singleton
public class DamageEvent implements Listener {
  private final PlayerDataManager playerDataManager;

  @Inject
  public DamageEvent(PlayerDataManager playerDataManager) {
    this.playerDataManager = playerDataManager;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player damager)) {
      return;
    }
    SlothPlayer slothPlayer = playerDataManager.getPlayer(damager);
    if (slothPlayer == null) {
      return;
    }
    double multiplier = slothPlayer.getDmgMultiplier();
    if (multiplier < 1.0) {
      event.setDamage(event.getDamage() * multiplier);
    }
  }
}
