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
package space.kaelus.sloth.utils.latency

import com.github.retrooper.packetevents.netty.channel.ChannelHelper
import java.util.ArrayDeque
import java.util.ArrayList
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.player.SlothPlayer
import space.kaelus.sloth.utils.Message
import space.kaelus.sloth.utils.MessageUtil

class LatencyUtils(private val player: SlothPlayer, private val plugin: SlothAC) : ILatencyUtils {
  private data class TransactionTask(val transactionId: Int, val task: Runnable)

  private val transactionMap: ArrayDeque<TransactionTask> = ArrayDeque()

  override fun addRealTimeTask(transaction: Int, runnable: Runnable) {
    addRealTimeTaskInternal(transaction, false, runnable)
  }

  override fun addRealTimeTaskAsync(transaction: Int, runnable: Runnable) {
    addRealTimeTaskInternal(transaction, true, runnable)
  }

  private fun addRealTimeTaskInternal(transactionId: Int, async: Boolean, runnable: Runnable) {
    if (player.transactions.lastTransactionReceived.get() >= transactionId) {
      if (async) {
        ChannelHelper.runInEventLoop(player.user.channel, runnable)
      } else {
        runnable.run()
      }
      return
    }
    synchronized(transactionMap) { transactionMap.add(TransactionTask(transactionId, runnable)) }
  }

  override fun handleNettySyncTransaction(receivedTransactionId: Int) {
    val tasksToRun = ArrayList<Runnable>()
    synchronized(transactionMap) {
      val iterator = transactionMap.iterator()
      while (iterator.hasNext()) {
        val taskEntry = iterator.next()
        val taskTransactionId = taskEntry.transactionId

        if (receivedTransactionId + 1 < taskTransactionId) {
          break
        }

        if (receivedTransactionId == taskTransactionId - 1) {
          continue
        }

        tasksToRun.add(taskEntry.task)
        iterator.remove()
      }
    }

    for (runnable in tasksToRun) {
      try {
        runnable.run()
      } catch (ex: Exception) {
        plugin.logger.severe(
          "An error occurred when running transactions for player: ${player.user.name}"
        )
        ex.printStackTrace()
        player.disconnect(MessageUtil.getMessage(Message.INTERNAL_ERROR))
      }
    }
  }
}
