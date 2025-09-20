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
package space.kaelus.sloth.checks.impl.ai;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.checks.AbstractCheck;
import space.kaelus.sloth.checks.CheckData;
import space.kaelus.sloth.checks.type.PacketCheck;
import space.kaelus.sloth.data.DataSession;
import space.kaelus.sloth.data.TickData;
import space.kaelus.sloth.player.SlothPlayer;

@CheckData(name = "DataCollector_Internal")
public class DataCollectorCheck extends AbstractCheck implements PacketCheck {
  private final DataCollectorManager dataCollectorManager;
  private final SlothAC plugin;

  public DataCollectorCheck(
      SlothPlayer slothPlayer, DataCollectorManager dataCollectorManager, SlothAC plugin) {
    super(slothPlayer);
    this.dataCollectorManager = dataCollectorManager;
    this.plugin = plugin;
  }

  @Override
  public void onPacketReceive(PacketReceiveEvent event) {
    if (slothPlayer == null) return;
    DataSession session = dataCollectorManager.getSession(slothPlayer.getUuid());
    if (session == null) return;
    if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
      if (slothPlayer.packetStateData.lastPacketWasTeleport
          || slothPlayer.packetStateData.lastPacketWasServerRotation) {
        plugin
            .getLogger()
            .info(
                "Skipping server-side rotation packet in data collection for player: "
                    + slothPlayer.getPlayer().getName());
        return;
      }

      if (slothPlayer.getTicksSinceAttack() < 40) {
        session.addTick(new TickData(slothPlayer));
      }
    }
  }
}
