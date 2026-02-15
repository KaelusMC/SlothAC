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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package space.kaelus.sloth.api.event

/** Functional listener for [SlothEventBus] subscriptions. */
fun interface SlothEventListener<T : SlothEvent> {
  fun handle(event: T)
}

/**
 * Lightweight event bus for SlothAC events.
 *
 * Events are dispatched on the thread that calls [post].
 */
interface SlothEventBus {
  /** Post an event to all registered listeners. */
  fun post(event: SlothEvent)

  /** Subscribe with default priority (0) and ignoreCancelled=false. */
  fun <T : SlothEvent> subscribe(
    pluginContext: Any,
    eventType: Class<T>,
    listener: SlothEventListener<T>,
  )

  /** Subscribe with explicit priority and ignoreCancelled. */
  fun <T : SlothEvent> subscribe(
    pluginContext: Any,
    eventType: Class<T>,
    listener: SlothEventListener<T>,
    priority: Int,
    ignoreCancelled: Boolean,
  )

  /** Unregister a specific listener for a context. */
  fun unregisterListener(pluginContext: Any, listener: SlothEventListener<*>)

  /** Unregister all listeners for a context. */
  fun unregisterAll(pluginContext: Any)
}
