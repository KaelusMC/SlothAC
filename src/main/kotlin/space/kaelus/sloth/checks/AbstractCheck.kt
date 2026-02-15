/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2026 KaelusMC
 *
 * This file contains code derived from GrimAC.
 * The original authors of GrimAC are credited below.
 *
 * Copyright (c) 2021-2026 GrimAC, DefineOutside and contributors.
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
package space.kaelus.sloth.checks

import space.kaelus.sloth.api.event.CheckFlagEvent
import space.kaelus.sloth.player.SlothPlayer

abstract class AbstractCheck(protected val slothPlayer: SlothPlayer) : ICheck {
  private val checkNameInternal: String
  val configName: String

  override val checkName: String
    get() = checkNameInternal

  init {
    val data = javaClass.getAnnotation(CheckData::class.java)
    checkNameInternal = data.name
    configName = if (data.configName == "DEFAULT") data.name else data.configName
  }

  protected fun flag(debug: String) {
    val event =
      CheckFlagEvent(slothPlayer.uuid, slothPlayer.player.name, checkName, configName, debug)
    slothPlayer.eventBus.post(event)
    if (event.cancelled) {
      return
    }
    slothPlayer.punishmentManager.handleFlag(this, debug)
  }
}
