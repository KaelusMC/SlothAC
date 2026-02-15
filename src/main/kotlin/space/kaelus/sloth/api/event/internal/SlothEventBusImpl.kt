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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package space.kaelus.sloth.api.event.internal

import java.util.Arrays
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import space.kaelus.sloth.api.event.SlothCancellableEvent
import space.kaelus.sloth.api.event.SlothEvent
import space.kaelus.sloth.api.event.SlothEventBus
import space.kaelus.sloth.api.event.SlothEventListener

class SlothEventBusImpl : SlothEventBus {
  private val listenerMap:
    ConcurrentHashMap<Class<out SlothEvent>, AtomicReference<Array<ListenerEntry>>> =
    ConcurrentHashMap()
  private val contextListeners: ConcurrentHashMap<Any, MutableSet<ListenerEntry>> =
    ConcurrentHashMap()

  override fun post(event: SlothEvent) {
    var currentType: Class<*>? = event.javaClass
    while (currentType != null && SlothEvent::class.java.isAssignableFrom(currentType)) {
      @Suppress("UNCHECKED_CAST")
      val ref =
        listenerMap[currentType as Class<out SlothEvent>]
          ?: run {
            currentType = currentType.superclass
            continue
          }
      val listeners = ref.get()
      for (listener in listeners) {
        if (event is SlothCancellableEvent && event.cancelled && !listener.ignoreCancelled) {
          continue
        }
        @Suppress("TooGenericExceptionCaught", "PrintStackTrace")
        try {
          listener.listener.handle(event)
        } catch (throwable: Throwable) {
          throwable.printStackTrace()
        }
      }
      currentType = currentType.superclass
    }
  }

  override fun <T : SlothEvent> subscribe(
    pluginContext: Any,
    eventType: Class<T>,
    listener: SlothEventListener<T>,
  ) {
    subscribe(pluginContext, eventType, listener, 0, false)
  }

  override fun <T : SlothEvent> subscribe(
    pluginContext: Any,
    eventType: Class<T>,
    listener: SlothEventListener<T>,
    priority: Int,
    ignoreCancelled: Boolean,
  ) {
    @Suppress("UNCHECKED_CAST")
    val entry =
      ListenerEntry(
        pluginContext = pluginContext,
        listener = listener as SlothEventListener<SlothEvent>,
        priority = priority,
        ignoreCancelled = ignoreCancelled,
      )
    addListener(eventType, entry)
    contextListeners.compute(pluginContext) { _, existing ->
      val set = existing ?: ConcurrentHashMap.newKeySet()
      set.add(entry)
      set
    }
  }

  override fun unregisterListener(pluginContext: Any, listener: SlothEventListener<*>) {
    val listeners = contextListeners[pluginContext] ?: return
    val toRemove = listeners.filter { it.listener === listener }.toSet()
    if (toRemove.isEmpty()) return
    for (entry in listenerMap.entries) {
      removeListeners(entry.key, entry.value) { toRemove.contains(it) }
    }
    listeners.removeAll(toRemove)
  }

  override fun unregisterAll(pluginContext: Any) {
    val listeners = contextListeners.remove(pluginContext) ?: return
    for (entry in listenerMap.entries) {
      removeListeners(entry.key, entry.value) { listeners.contains(it) }
    }
  }

  private fun addListener(eventType: Class<out SlothEvent>, newListener: ListenerEntry) {
    val ref = listenerMap.computeIfAbsent(eventType) { AtomicReference(emptyArray()) }
    while (true) {
      val oldArray = ref.get()
      var insertionPoint =
        Arrays.binarySearch(oldArray, newListener) { a, b -> b.priority.compareTo(a.priority) }
      insertionPoint =
        if (insertionPoint < 0) {
          -(insertionPoint + 1)
        } else {
          while (
            insertionPoint < oldArray.size - 1 &&
              oldArray[insertionPoint + 1].priority == newListener.priority
          ) {
            insertionPoint++
          }
          insertionPoint + 1
        }

      val newArray = arrayOfNulls<ListenerEntry>(oldArray.size + 1)
      System.arraycopy(oldArray, 0, newArray, 0, insertionPoint)
      newArray[insertionPoint] = newListener
      System.arraycopy(
        oldArray,
        insertionPoint,
        newArray,
        insertionPoint + 1,
        oldArray.size - insertionPoint,
      )
      @Suppress("UNCHECKED_CAST") val castArray = newArray as Array<ListenerEntry>
      if (ref.compareAndSet(oldArray, castArray)) {
        break
      }
    }
  }

  private fun removeListeners(
    eventType: Class<out SlothEvent>,
    ref: AtomicReference<Array<ListenerEntry>>,
    filter: (ListenerEntry) -> Boolean,
  ) {
    while (true) {
      val oldArray = ref.get()
      var remaining = 0
      for (listener in oldArray) {
        if (!filter(listener)) {
          remaining++
        }
      }
      if (remaining == oldArray.size) {
        return
      }
      val newArray = arrayOfNulls<ListenerEntry>(remaining)
      var index = 0
      for (listener in oldArray) {
        if (!filter(listener)) {
          newArray[index++] = listener
        }
      }
      @Suppress("UNCHECKED_CAST") val castArray = newArray as Array<ListenerEntry>
      if (ref.compareAndSet(oldArray, castArray)) {
        if (castArray.isEmpty()) {
          listenerMap.remove(eventType)
        }
        break
      }
    }
  }

  private data class ListenerEntry(
    val pluginContext: Any,
    val listener: SlothEventListener<SlothEvent>,
    val priority: Int,
    val ignoreCancelled: Boolean,
  )
}
