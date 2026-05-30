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
package space.kaelus.sloth.command.commands.admin

import java.net.URI
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.kotlin.extension.buildAndRegister
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.command.SlothCommand
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.connect.ConnectService
import space.kaelus.sloth.connect.Credentials
import space.kaelus.sloth.connect.CredentialsStore
import space.kaelus.sloth.connect.PollResult
import space.kaelus.sloth.connect.RevokeResult
import space.kaelus.sloth.connect.StartResult
import space.kaelus.sloth.scheduler.SchedulerService
import space.kaelus.sloth.sender.Sender
import space.kaelus.sloth.server.AIServerProvider
import space.kaelus.sloth.utils.Message
import space.kaelus.sloth.utils.MessageUtil

@Suppress("TooManyFunctions", "ReturnCount")
class ConnectCommand(
  private val plugin: SlothAC,
  private val connectService: ConnectService,
  private val credentialsStore: CredentialsStore,
  private val configManager: ConfigManager,
  private val aiServerProvider: AIServerProvider,
  private val scheduler: SchedulerService,
) : SlothCommand {

  private val active = AtomicReference<ConnectSession?>(null)
  private val pendingDisconnect = ConcurrentHashMap<UUID, Long>()

  override fun register(manager: CommandManager<Sender>) {
    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("connect").permission(PERMISSION).handler(this@ConnectCommand::connect)
    }
    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("disconnect").permission(PERMISSION).handler(this@ConnectCommand::disconnect)
    }
    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("disconnect")
        .literal("confirm")
        .permission(PERMISSION)
        .handler(this@ConnectCommand::disconnectConfirm)
    }
    manager.buildAndRegister("sloth", aliases = arrayOf("slothac")) {
      literal("connect")
        .literal("status")
        .permission(PERMISSION)
        .handler(this@ConnectCommand::status)
    }
  }

  private fun connect(context: CommandContext<Sender>) {
    val sender = context.sender()
    val native = sender.nativeSender
    if (!sender.isTrustedConsole && credentialsStore.isLinked()) {
      MessageUtil.sendMessage(native, Message.CONNECT_CONSOLE_ONLY)
      return
    }
    if (!checkPanelUrl(native)) {
      return
    }

    supersedeActive()?.let { MessageUtil.sendMessage(native, Message.CONNECT_RESTARTED) }

    val uuid = sender.uniqueId
    val isConsole = sender.isConsole
    scheduler.runAsync {
      when (val result = connectService.start()) {
        is StartResult.Started -> {
          val session =
            ConnectSession(
              deviceCode = result.deviceCode,
              intervalSeconds = AtomicLong(result.intervalSeconds.coerceAtLeast(1)),
              deadlineEpochSec = Instant.now().epochSecond + result.expiresInSeconds,
              senderUuid = uuid,
              isConsole = isConsole,
            )
          if (!active.compareAndSet(null, session)) {
            scheduler.runAsync { connectService.cancel(session.deviceCode) }
            return@runAsync
          }
          notify(session) { sendStartMessages(it, result) }
          scheduleNextPoll(session)
        }
        is StartResult.Error ->
          notify(uuid, isConsole) {
            MessageUtil.sendMessage(it, Message.CONNECT_ERROR, "reason", result.message)
          }
      }
    }
  }

  private fun disconnect(context: CommandContext<Sender>) {
    val sender = context.sender()
    val native = sender.nativeSender
    if (!sender.isTrustedConsole) {
      MessageUtil.sendMessage(native, Message.CONNECT_CONSOLE_ONLY)
      return
    }
    if (credentialsStore.read() == null) {
      MessageUtil.sendMessage(native, Message.CONNECT_DISCONNECT_NOTHING)
      return
    }
    pendingDisconnect[sender.uniqueId] = Instant.now().epochSecond + CONFIRM_WINDOW_SECONDS
    MessageUtil.sendMessage(native, Message.CONNECT_DISCONNECT_CONFIRM)
  }

  private fun disconnectConfirm(context: CommandContext<Sender>) {
    val sender = context.sender()
    val native = sender.nativeSender
    if (!sender.isTrustedConsole) {
      MessageUtil.sendMessage(native, Message.CONNECT_CONSOLE_ONLY)
      return
    }
    val deadline = pendingDisconnect.remove(sender.uniqueId)
    if (deadline == null || Instant.now().epochSecond > deadline) {
      MessageUtil.sendMessage(native, Message.CONNECT_DISCONNECT_NO_PENDING)
      return
    }
    performDisconnect(sender)
  }

  private fun performDisconnect(sender: Sender) {
    val native = sender.nativeSender
    supersedeActive()

    val credentials = credentialsStore.read()
    if (credentials == null) {
      MessageUtil.sendMessage(native, Message.CONNECT_DISCONNECT_NOTHING)
      return
    }

    val canRevoke = configManager.connectPanelUrl.let { it.isNotBlank() && isSecureUrl(it) }
    val uuid = sender.uniqueId
    val isConsole = sender.isConsole
    scheduler.runAsync {
      val revoked = canRevoke && connectService.revoke(credentials.secretKey) !is RevokeResult.Error
      if (!revoked) {
        plugin.logger.warning(
          "[Connect] Panel revoke not confirmed; clearing local credentials anyway. " +
            "Revoke this server in the panel manually."
        )
      }
      credentialsStore.clear()
      applyConfigReload()
      val message =
        if (revoked) Message.CONNECT_DISCONNECT_SUCCESS else Message.CONNECT_DISCONNECT_LOCAL_ONLY
      notify(uuid, isConsole) { MessageUtil.sendMessage(it, message) }
    }
  }

  private fun status(context: CommandContext<Sender>) {
    val native = context.sender().nativeSender
    val credentials = credentialsStore.read()
    if (credentials == null) {
      MessageUtil.sendMessage(native, Message.CONNECT_STATUS_NOT_LINKED)
      return
    }
    MessageUtil.sendMessage(native, Message.CONNECT_STATUS_HEADER)
    MessageUtil.sendMessage(
      native,
      Message.CONNECT_STATUS_LINKED,
      "server",
      credentials.serverName ?: credentials.serverId ?: "unknown",
    )
    MessageUtil.sendMessage(
      native,
      Message.CONNECT_STATUS_KEY,
      "key",
      maskKey(credentials.secretKey),
    )
    MessageUtil.sendMessage(
      native,
      Message.CONNECT_STATUS_SERVER_URL,
      "url",
      configManager.aiServerUrl,
    )
  }

  private fun scheduleNextPoll(session: ConnectSession) {
    val delayMs = session.intervalSeconds.get() * MILLIS_PER_SECOND
    scheduler.runLaterAsync({ pollOnce(session) }, delayMs)
  }

  private fun pollOnce(session: ConnectSession) {
    if (isStale(session)) {
      return
    }
    if (Instant.now().epochSecond >= session.deadlineEpochSec) {
      finish(session)
      notify(session) { MessageUtil.sendMessage(it, Message.CONNECT_EXPIRED) }
      return
    }
    val result = connectService.poll(session.deviceCode)
    if (isStale(session)) {
      return
    }
    when (result) {
      PollResult.Pending -> scheduleNextPoll(session)
      is PollResult.SlowDown -> {
        val next = result.intervalSeconds.coerceAtLeast(session.intervalSeconds.get())
        session.intervalSeconds.set(next)
        scheduleNextPoll(session)
      }
      is PollResult.Approved -> {
        if (finish(session)) applyApproved(session, result)
      }
      PollResult.Denied -> {
        finish(session)
        notify(session) { MessageUtil.sendMessage(it, Message.CONNECT_DENIED) }
      }
      PollResult.Expired -> {
        finish(session)
        notify(session) { MessageUtil.sendMessage(it, Message.CONNECT_EXPIRED) }
      }
      is PollResult.Error -> {
        plugin.logger.fine("[Connect] poll error: ${result.message}")
        scheduleNextPoll(session)
      }
    }
  }

  private fun applyApproved(session: ConnectSession, approved: PollResult.Approved) {
    credentialsStore.write(
      Credentials(
        secretKey = approved.secretKey,
        serverId = approved.serverId,
        serverName = approved.serverName,
        allowlistedIp = approved.allowlistedIp,
      )
    )
    applyConfigReload()
    val message =
      if (approved.needsPlan) Message.CONNECT_SUCCESS_NEEDS_PLAN else Message.CONNECT_SUCCESS
    notify(session) {
      MessageUtil.sendMessage(it, message, "server", approved.serverName ?: "your server")
    }
  }

  private fun applyConfigReload() {
    scheduler.runSync {
      configManager.reloadConfig()
      aiServerProvider.reload()
    }
  }

  private fun supersedeActive(): ConnectSession? {
    val previous = active.getAndSet(null) ?: return null
    previous.cancelled.set(true)
    val deviceCode = previous.deviceCode
    scheduler.runAsync { connectService.cancel(deviceCode) }
    return previous
  }

  private fun finish(session: ConnectSession): Boolean = active.compareAndSet(session, null)

  private fun isStale(session: ConnectSession): Boolean =
    session.cancelled.get() || active.get() !== session

  private fun checkPanelUrl(sender: CommandSender): Boolean {
    val url = configManager.connectPanelUrl
    if (url.isBlank()) {
      MessageUtil.sendMessage(sender, Message.CONNECT_DISABLED)
      return false
    }
    if (!isSecureUrl(url)) {
      MessageUtil.sendMessage(sender, Message.CONNECT_INSECURE_URL)
      return false
    }
    return true
  }

  private fun isSecureUrl(url: String): Boolean =
    try {
      val uri = URI.create(url.trim())
      val host = uri.host?.lowercase()?.removeSurrounding("[", "]")
      when (uri.scheme?.lowercase()) {
        "https" -> true
        "http" -> host == "localhost" || host == "127.0.0.1" || host == "::1"
        else -> false
      }
    } catch (_: Exception) {
      false
    }

  private fun sendStartMessages(sender: CommandSender, result: StartResult.Started) {
    val base = configManager.connectPanelUrl.trim().trimEnd('/')
    val verifyUrl = "$base/connect"
    val resolver =
      TagResolver.resolver(
        MessageUtil.clickUrlTag("link", "$verifyUrl?code=${result.userCode}"),
        Placeholder.unparsed("code", result.userCode),
      )
    MessageUtil.sendMessage(sender, MessageUtil.getMessage(Message.CONNECT_START, resolver))
    MessageUtil.sendMessage(sender, Message.CONNECT_URL, "url", verifyUrl, "code", result.userCode)
    MessageUtil.sendMessage(sender, Message.CONNECT_WAITING)
  }

  private fun notify(session: ConnectSession, action: (CommandSender) -> Unit) =
    notify(session.senderUuid, session.isConsole, action)

  private fun notify(uuid: UUID, isConsole: Boolean, action: (CommandSender) -> Unit) {
    scheduler.runSync {
      val target: CommandSender? =
        if (isConsole) Bukkit.getConsoleSender() else Bukkit.getPlayer(uuid)
      if (target != null) {
        action(target)
      } else {
        plugin.logger.info("[Connect] Recipient offline; a connect message was not delivered.")
      }
    }
  }

  private fun maskKey(key: String): String =
    if (key.length <= MASK_VISIBLE) "•".repeat(key.length)
    else "••••••••" + key.takeLast(MASK_VISIBLE)

  private class ConnectSession(
    val deviceCode: String,
    val intervalSeconds: AtomicLong,
    val deadlineEpochSec: Long,
    val senderUuid: UUID,
    val isConsole: Boolean,
    val cancelled: AtomicBoolean = AtomicBoolean(false),
  )

  private companion object {
    const val PERMISSION = "sloth.connect"
    const val MILLIS_PER_SECOND = 1000L
    const val MASK_VISIBLE = 4
    const val CONFIRM_WINDOW_SECONDS = 30L
  }
}
