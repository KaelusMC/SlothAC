/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2025 KaelusMC
 *
 * This file contains code derived from GrimAC.
 * The original authors of GrimAC are credited below.
 *
 * Copyright (c) 2021-2025 GrimAC, DefineOutside and contributors.
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
package space.kaelus.sloth.checks;

import lombok.Getter;
import space.kaelus.sloth.player.SlothPlayer;

@Getter
public abstract class AbstractCheck implements ICheck {
  protected final SlothPlayer slothPlayer;
  private final String checkName;
  private final String configName;

  public AbstractCheck(SlothPlayer slothPlayer) {
    this.slothPlayer = slothPlayer;
    CheckData data = getClass().getAnnotation(CheckData.class);
    this.checkName = data.name();
    this.configName = data.configName().equals("DEFAULT") ? data.name() : data.configName();
  }

  protected void flag(String debug) {
    slothPlayer.getPunishmentManager().handleFlag(this, debug);
  }
}
