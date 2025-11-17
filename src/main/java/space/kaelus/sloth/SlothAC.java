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
package space.kaelus.sloth;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;
import space.kaelus.sloth.di.DaggerSlothComponent;
import space.kaelus.sloth.di.SlothComponent;

public final class SlothAC extends JavaPlugin {

  private SlothComponent component;
  private SlothCore core;

  @Override
  public void onLoad() {
    PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
    PacketEvents.getAPI().getSettings().checkForUpdates(false);
    PacketEvents.getAPI().load();
  }

  @Override
  public void onEnable() {
    this.component = DaggerSlothComponent.builder().plugin(this).build();
    this.core = this.component.core();

    this.core.enable();
  }

  @Override
  public void onDisable() {
    if (this.core != null) {
      this.core.disable();
    }
  }

  public void onReload() {
    if (this.core != null) {
      this.core.reload();
    }
  }
}
