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

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import space.kaelus.sloth.checks.type.PacketCheck
import space.kaelus.sloth.checks.type.RotationCheck
import space.kaelus.sloth.player.SlothPlayer
import space.kaelus.sloth.utils.update.RotationUpdate

class CheckManager(player: SlothPlayer, checkFactories: Set<CheckFactory>) {
  private val rotationChecks = ArrayList<RotationCheck>()
  private val packetChecks = ArrayList<PacketCheck>()
  private val checks = HashMap<Class<out ICheck>, ICheck>()

  init {
    for (factory in checkFactories) {
      registerCheck(factory.create(player))
    }
  }

  private fun registerCheck(check: ICheck) {
    checks[check.javaClass] = check

    if (check is RotationCheck) {
      rotationChecks.add(check)
    }

    if (check is PacketCheck) {
      packetChecks.add(check)
    }
  }

  fun reloadChecks() {
    for (check in checks.values) {
      if (check is Reloadable) {
        check.reload()
      }
    }
  }

  fun onRotationUpdate(update: RotationUpdate) {
    for (check in rotationChecks) {
      check.process(update)
    }
  }

  fun onPacketReceive(event: PacketReceiveEvent) {
    for (check in packetChecks) {
      check.onPacketReceive(event)
    }
  }

  @Suppress("UNCHECKED_CAST")
  fun <T : ICheck> getCheck(clazz: Class<T>): T? {
    return checks[clazz] as T?
  }

  fun getAllChecks(): Collection<ICheck> = checks.values

  fun interface Factory {
    fun create(player: SlothPlayer): CheckManager
  }
}
