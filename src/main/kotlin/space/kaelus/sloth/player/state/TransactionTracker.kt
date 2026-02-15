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
package space.kaelus.sloth.player.state

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.player.User
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation
import it.unimi.dsi.fastutil.ints.IntArraySet
import java.util.Queue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import space.kaelus.sloth.player.TransactionStamp

class TransactionTracker {
  val transactionsSent: Queue<TransactionStamp> = ConcurrentLinkedQueue()
  val entitiesDespawnedThisTransaction: IntArraySet = IntArraySet()
  val didWeSendThatTrans: MutableSet<Short> = ConcurrentHashMap.newKeySet<Short>()
  val lastTransactionSent: AtomicInteger = AtomicInteger(0)
  val lastTransactionReceived: AtomicInteger = AtomicInteger(0)
  private val transactionIdCounter: AtomicInteger = AtomicInteger(0)

  fun sendTransaction(user: User) {
    if (user.connectionState != com.github.retrooper.packetevents.protocol.ConnectionState.PLAY) {
      return
    }

    val transactionId = (-1 * (transactionIdCounter.getAndIncrement() and 0x7FFF)).toShort()
    didWeSendThatTrans.add(transactionId)

    val packet =
      if (
        PacketEvents.getAPI()
          .serverManager
          .version
          .isNewerThanOrEquals(
            com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_17
          )
      ) {
        WrapperPlayServerPing(transactionId.toInt())
      } else {
        WrapperPlayServerWindowConfirmation(0, transactionId, false)
      }
    user.sendPacket(packet)
  }
}
