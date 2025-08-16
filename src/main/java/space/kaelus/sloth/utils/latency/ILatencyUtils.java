/*
 * This file is part of GrimAC - https://github.com/GrimAnticheat/Grim
 * Copyright (C) 2021-2025 GrimAC, DefineOutside and contributors
 *
 * GrimAC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GrimAC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package space.kaelus.sloth.utils.latency;


public interface ILatencyUtils {
    /**
     * Adds a task to be executed when the corresponding transaction ACK is received.
     *
     * @param transaction The transaction ID this task is associated with.
     * @param runnable The task to execute.
     */
    void addRealTimeTask(int transaction, Runnable runnable);

    /**
     * Adds a task to be executed asynchronously via the player's event loop
     * when the corresponding transaction ACK is received.
     * (Note: Benchmark might simplify/ignore the async part unless specifically testing event loop contention)
     *
     * @param transaction The transaction ID this task is associated with.
     * @param runnable The task to execute.
     */
    void addRealTimeTaskAsync(int transaction, Runnable runnable);

    /**
     * Processes received transaction ACKs and runs associated tasks.
     *
     * @param receivedTransactionId The ID of the transaction ACK received from the client.
     */
    void handleNettySyncTransaction(int receivedTransactionId);
}