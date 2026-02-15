/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2026 KaelusMC
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
package space.kaelus.sloth.api.internal

import space.kaelus.sloth.api.SlothApi
import space.kaelus.sloth.api.event.SlothEventBus
import space.kaelus.sloth.api.service.AiApi
import space.kaelus.sloth.api.service.CheckApi
import space.kaelus.sloth.api.service.MonitorApi
import space.kaelus.sloth.api.service.PunishmentApi

class SlothApiImpl(
  private val aiApi: AiApi,
  private val checkApi: CheckApi,
  private val punishmentApi: PunishmentApi,
  private val monitorApi: MonitorApi,
  private val eventBus: SlothEventBus,
) : SlothApi {
  override fun ai(): AiApi = aiApi

  override fun checks(): CheckApi = checkApi

  override fun punishments(): PunishmentApi = punishmentApi

  override fun monitor(): MonitorApi = monitorApi

  override fun events(): SlothEventBus = eventBus
}
