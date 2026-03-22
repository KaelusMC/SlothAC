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
package space.kaelus.sloth.monitor

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.manager.server.ServerVersion
import com.github.retrooper.packetevents.protocol.score.ScoreFormat
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisplayScoreboard
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerResetScore
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateScore
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.roundToInt
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.checks.impl.ai.AiCheck
import space.kaelus.sloth.config.ConfigView
import space.kaelus.sloth.platform.scheduler.TaskHandle
import space.kaelus.sloth.player.PlayerDataManager
import space.kaelus.sloth.scheduler.SchedulerService
import space.kaelus.sloth.utils.MessageUtil

@Suppress("TooManyFunctions")
internal class ViewSessionCoordinator(
  private val plugin: SlothAC,
  private val playerDataManager: PlayerDataManager,
  private val scheduler: SchedulerService,
) {
  private val sessions = ConcurrentHashMap<UUID, ViewSession>()
  private val componentCache = ViewComponentCache()
  private val packetBridge = ViewTeamPacketBridge(componentCache)
  private val belowNameBridge = ViewBelowNamePacketBridge(componentCache)
  private val tagRenderer = ViewTagRenderer(playerDataManager)

  fun hasSession(viewerId: UUID): Boolean = sessions.containsKey(viewerId)

  fun session(viewerId: UUID): ViewSession? = sessions[viewerId]

  fun activeViewerIds(): List<UUID> = sessions.keys.toList()

  fun enable(viewer: Player, config: ViewRuntimeConfig) {
    val viewerId = viewer.uniqueId
    if (sessions.containsKey(viewerId)) {
      return
    }

    val objectiveName =
      if (config.placement == ViewPlacement.BELOW_NAME) {
        objectiveNameForViewer(viewerId)
      } else {
        null
      }

    val session = ViewSession(config, config.placement, objectiveName)
    sessions[viewerId] = session

    if (session.usesBelowName()) {
      belowNameBridge.createObjective(
        viewer,
        objectiveName!!,
        config.belowTitle,
        config.defaultBelowText,
      )
    }

    bootstrapTrackedTargets(viewer, session)
    refreshTrackedTargets(viewer, session)
    session.task =
      scheduler.runTimer(
        viewer,
        Runnable { refreshViewer(viewerId, ::resolveViewer) },
        0L,
        config.updateTicks,
      )
  }

  fun reload(config: ViewRuntimeConfig) {
    val viewerIds = activeViewerIds()
    for (viewerId in viewerIds) {
      val viewer = Bukkit.getPlayer(viewerId)
      disable(viewerId, viewer)
      if (viewer != null && viewer.isOnline && viewer.hasPermission(VIEW_PERMISSION)) {
        enable(viewer, config)
      }
    }
  }

  fun refreshViewer(viewerId: UUID, viewerResolver: (UUID) -> Player?) {
    val session = sessions[viewerId] ?: return
    val viewer = viewerResolver(viewerId) ?: return

    if (session.shouldResync()) {
      resyncTrackedTargets(viewer, session)
    }
    refreshTrackedTargets(viewer, session)
  }

  fun trackTarget(viewerId: UUID, target: Player) {
    val session = sessions[viewerId] ?: return
    session.updateTrackedName(target.uniqueId, target.name)
    session.targetTeams.computeIfAbsent(target.uniqueId) { TargetTeamState(teamNameForView(it)) }
  }

  fun removeTrackedTarget(viewerId: UUID, targetId: UUID, fallbackTargetName: String?) {
    val session = sessions[viewerId] ?: return
    val viewer = resolveViewer(viewerId) ?: return
    removeTrackedTarget(viewer, session, targetId, fallbackTargetName)
  }

  fun removeTargetFromAllSessions(targetId: UUID, targetName: String) {
    for ((viewerId, session) in sessions.entries) {
      val viewer = Bukkit.getPlayer(viewerId)
      if (viewer != null && viewer.isOnline) {
        removeTrackedTarget(viewer, session, targetId, targetName)
      } else {
        session.targetTeams.remove(targetId)
        session.removeTrackedName(targetId, targetName)
      }
    }
  }

  fun disable(viewerId: UUID, viewerHint: Player?) {
    val session = sessions.remove(viewerId) ?: return
    session.task?.cancel()

    val viewer = viewerHint ?: Bukkit.getPlayer(viewerId)
    if (viewer != null && viewer.isOnline) {
      if (session.usesBelowName()) {
        val objectiveName = session.belowObjectiveName
        if (objectiveName != null) {
          belowNameBridge.removeObjective(viewer, objectiveName)
        }
      } else {
        for (state in session.targetTeams.values) {
          packetBridge.removeTeam(viewer, state.teamName)
        }
      }
    }

    session.clearTargets()
  }

  fun reassertBelowNameDisplay(
    viewerId: UUID,
    conflictingObjective: String,
    viewerResolver: (UUID) -> Player?,
  ) {
    val session = sessions[viewerId]
    if (session != null && session.usesBelowName()) {
      val viewer = viewerResolver(viewerId)
      val objectiveName = session.belowObjectiveName
      if (viewer != null && objectiveName != null) {
        if (!session.belowNameConflictLogged) {
          plugin.logger.warning(
            "[View] Viewer ${viewer.name} reasserted Sloth below-name display after " +
              "'$conflictingObjective' attempted to claim the slot."
          )
          session.belowNameConflictLogged = true
        }

        belowNameBridge.displayObjective(viewer, objectiveName)
        refreshTrackedTargets(viewer, session)
      }
    }
  }

  fun recreateBelowNameObjective(
    viewerId: UUID,
    objectiveName: String,
    viewerResolver: (UUID) -> Player?,
  ) {
    val session = sessions[viewerId]
    val shouldRecreate =
      session != null && session.usesBelowName() && session.belowObjectiveName == objectiveName
    if (shouldRecreate) {
      val viewer = viewerResolver(viewerId)
      if (viewer != null) {
        session.targetTeams.values.forEach(TargetTeamState::invalidateBelowName)
        belowNameBridge.createObjective(
          viewer,
          objectiveName,
          session.config.belowTitle,
          session.config.defaultBelowText,
        )
        refreshTrackedTargets(viewer, session)
      }
    }
  }

  private fun refreshTrackedTargets(viewer: Player, session: ViewSession) {
    for ((targetUuid, state) in session.targetTeams.entries.toList()) {
      val target = Bukkit.getPlayer(targetUuid)
      if (target == null || !shouldTrackTarget(viewer, target)) {
        removeTrackedTarget(viewer, session, targetUuid, target?.name)
        continue
      }
      updateTarget(viewer, session, target, state)
    }
  }

  private fun bootstrapTrackedTargets(viewer: Player, session: ViewSession) {
    for (target in Bukkit.getOnlinePlayers()) {
      if (!shouldTrackTarget(viewer, target)) {
        continue
      }
      session.updateTrackedName(target.uniqueId, target.name)
      session.targetTeams.computeIfAbsent(target.uniqueId) { TargetTeamState(teamNameForView(it)) }
    }
  }

  private fun resyncTrackedTargets(viewer: Player, session: ViewSession) {
    val seenTargets = HashSet<UUID>()
    for (target in Bukkit.getOnlinePlayers()) {
      if (!shouldTrackTarget(viewer, target)) {
        continue
      }
      seenTargets.add(target.uniqueId)
      session.updateTrackedName(target.uniqueId, target.name)
      session.targetTeams.computeIfAbsent(target.uniqueId) { TargetTeamState(teamNameForView(it)) }
    }

    for (targetId in session.targetTeams.keys.toList()) {
      if (seenTargets.contains(targetId)) {
        continue
      }
      removeTrackedTarget(viewer, session, targetId, session.targetNameFor(targetId))
    }
  }

  private fun updateTarget(
    viewer: Player,
    session: ViewSession,
    target: Player,
    state: TargetTeamState,
  ) {
    val rendered = tagRenderer.render(target, state, session.config)

    if (session.usesBelowName()) {
      state.updateBelowName(
        viewer,
        session.belowObjectiveName,
        target.name,
        rendered,
        belowNameBridge,
      )
    } else {
      state.updateTeam(viewer, session.config.rebindCycles, target.name, rendered, packetBridge)
    }

    session.updateTrackedName(target.uniqueId, state.lastTargetName.ifBlank { target.name })
  }

  private fun removeTrackedTarget(
    viewer: Player,
    session: ViewSession,
    targetId: UUID,
    fallbackTargetName: String?,
  ) {
    val state = session.targetTeams.remove(targetId) ?: return
    session.removeTrackedName(targetId, fallbackTargetName)
    state.removeFromViewer(
      viewer,
      if (session.usesBelowName()) session.belowObjectiveName else null,
      fallbackTargetName.orEmpty(),
      belowNameBridge,
      packetBridge,
    )
  }

  private fun resolveViewer(viewerId: UUID): Player? = Bukkit.getPlayer(viewerId)

  private fun shouldTrackTarget(viewer: Player, target: Player): Boolean =
    target.isOnline && viewer.world.uid == target.world.uid && viewer.canSee(target)
}

internal class ViewSession(
  val config: ViewRuntimeConfig,
  var placement: ViewPlacement,
  var belowObjectiveName: String?,
) {
  val targetTeams = ConcurrentHashMap<UUID, TargetTeamState>()
  private val targetNamesById = ConcurrentHashMap<UUID, String>()
  private val targetIdsByName = ConcurrentHashMap<String, UUID>()

  var task: TaskHandle? = null
  var cyclesSinceResync: Int = 0
  var belowNameConflictLogged: Boolean = false

  fun usesBelowName(): Boolean = placement == ViewPlacement.BELOW_NAME && belowObjectiveName != null

  fun shouldResync(): Boolean {
    cyclesSinceResync++
    if (cyclesSinceResync < config.resyncCycles) {
      return false
    }
    cyclesSinceResync = 0
    return true
  }

  fun updateTrackedName(targetId: UUID, targetName: String) {
    if (targetName.isBlank()) {
      return
    }

    val previousName = targetNamesById.put(targetId, targetName)
    if (previousName != null && previousName != targetName) {
      targetIdsByName.remove(previousName, targetId)
    }
    targetIdsByName[targetName] = targetId
  }

  fun removeTrackedName(targetId: UUID, fallbackTargetName: String?) {
    val trackedName = targetNamesById.remove(targetId) ?: fallbackTargetName
    if (trackedName != null) {
      targetIdsByName.remove(trackedName, targetId)
    }
  }

  fun targetIdByName(name: String): UUID? = targetIdsByName[name]

  fun targetNameFor(targetId: UUID): String? = targetNamesById[targetId]

  fun clearTargets() {
    targetTeams.clear()
    targetNamesById.clear()
    targetIdsByName.clear()
  }
}

internal class TargetTeamState(val teamName: String) {
  var created: Boolean = false
  var lastPrefix: String = ""
  var lastSuffix: String = ""
  var lastBelow: String = ""
  var lastBelowScore: Int = 0
  var lastTargetName: String = ""

  private var cyclesSinceRebind: Int = 0
  private var pendingRebind: Boolean = false
  private var lastPingBucket: Int = Int.MIN_VALUE
  private var lastPingSample: String = ""
  private var cyclesSincePingRefresh: Int = Int.MAX_VALUE

  fun markRebindNeeded() {
    pendingRebind = true
  }

  fun invalidateBelowName() {
    created = false
    lastBelow = ""
    lastBelowScore = 0
  }

  fun resolvePingDisplay(ping: Int, config: ViewRuntimeConfig): String {
    if (!config.usesPing) {
      return ""
    }

    val shouldRefresh =
      cyclesSincePingRefresh >= config.pingRefreshCycles || lastPingBucket == Int.MIN_VALUE
    val pingSample =
      if (!shouldRefresh) {
        cyclesSincePingRefresh++
        lastPingSample
      } else {
        cyclesSincePingRefresh = 0
        val bucket = if (config.pingBucketMs <= 1) ping else ping / config.pingBucketMs
        if (bucket != lastPingBucket || lastPingSample.isBlank()) {
          lastPingBucket = bucket
          lastPingSample = ping.toString()
        }
        lastPingSample
      }

    return pingSample
  }

  fun updateBelowName(
    viewer: Player,
    objectiveName: String?,
    targetName: String,
    rendered: RenderedTag,
    belowNameBridge: ViewBelowNamePacketBridge,
  ) {
    val objective = objectiveName ?: return
    val belowScore = rendered.belowScore
    if (belowScore == null) {
      clearBelowName(viewer, objective, targetName, belowNameBridge)
      return
    }

    val nameChanged = lastTargetName.isNotBlank() && lastTargetName != targetName
    if (
      shouldUpdateBelowName(targetName, rendered, belowScore, belowNameBridge.supportsFancyText())
    ) {
      if (nameChanged) {
        belowNameBridge.removeEntry(viewer, objective, lastTargetName)
      }
      belowNameBridge.updateEntry(viewer, objective, targetName, rendered.below, belowScore)
      lastBelow = rendered.below
      lastBelowScore = belowScore
      lastTargetName = targetName
      created = true
    }
  }

  private fun clearBelowName(
    viewer: Player,
    objective: String,
    targetName: String,
    belowNameBridge: ViewBelowNamePacketBridge,
  ) {
    if (created) {
      val entryName = lastTargetName.ifBlank { targetName }
      if (entryName.isNotBlank()) {
        belowNameBridge.removeEntry(viewer, objective, entryName)
      }
    }
    lastBelow = ""
    lastBelowScore = 0
    lastTargetName = targetName
    created = false
  }

  private fun shouldUpdateBelowName(
    targetName: String,
    rendered: RenderedTag,
    belowScore: Int,
    fancyTextSupported: Boolean,
  ): Boolean {
    val displayChanged = fancyTextSupported && rendered.below != lastBelow
    val scoreChanged = belowScore != lastBelowScore
    val nameChanged = lastTargetName.isNotBlank() && lastTargetName != targetName
    return !created || displayChanged || scoreChanged || nameChanged
  }

  fun updateTeam(
    viewer: Player,
    rebindCycles: Int,
    targetName: String,
    rendered: RenderedTag,
    teamBridge: ViewTeamPacketBridge,
  ) {
    val nameChanged = created && lastTargetName.isNotBlank() && lastTargetName != targetName
    if (!created) {
      teamBridge.createTeam(viewer, teamName, targetName, rendered)
      apply(rendered)
      created = true
      cyclesSinceRebind = 0
      pendingRebind = false
      lastTargetName = targetName
      return
    }

    if (nameChanged) {
      teamBridge.removeTeam(viewer, teamName)
      teamBridge.createTeam(viewer, teamName, targetName, rendered)
      apply(rendered)
      cyclesSinceRebind = 0
      pendingRebind = false
      lastTargetName = targetName
      return
    }

    val changed = rendered.prefix != lastPrefix || rendered.suffix != lastSuffix
    if (changed) {
      teamBridge.updateTeam(viewer, teamName, rendered)
      apply(rendered)
      cyclesSinceRebind = 0
    } else {
      cyclesSinceRebind++
    }

    if (pendingRebind || cyclesSinceRebind >= rebindCycles) {
      teamBridge.rebindEntity(viewer, teamName, targetName)
      cyclesSinceRebind = 0
      pendingRebind = false
    }

    lastTargetName = targetName
  }

  fun removeFromViewer(
    viewer: Player,
    objectiveName: String?,
    fallbackTargetName: String,
    belowNameBridge: ViewBelowNamePacketBridge,
    teamBridge: ViewTeamPacketBridge,
  ) {
    if (objectiveName != null) {
      val entryName = lastTargetName.ifBlank { fallbackTargetName }
      if (entryName.isNotBlank()) {
        belowNameBridge.removeEntry(viewer, objectiveName, entryName)
      }
      return
    }
    teamBridge.removeTeam(viewer, teamName)
  }

  private fun apply(tag: RenderedTag) {
    lastPrefix = tag.prefix
    lastSuffix = tag.suffix
  }
}

internal class ViewTeamPacketBridge(private val componentCache: ViewComponentCache) {
  fun createTeam(viewer: Player, teamName: String, playerName: String, rendered: RenderedTag) {
    val wrapper =
      WrapperPlayServerTeams(
        teamName,
        WrapperPlayServerTeams.TeamMode.CREATE,
        createTeamInfo(rendered),
        listOf(playerName),
      )
    sendPacket(viewer, wrapper)
  }

  fun updateTeam(viewer: Player, teamName: String, rendered: RenderedTag) {
    val wrapper =
      WrapperPlayServerTeams(
        teamName,
        WrapperPlayServerTeams.TeamMode.UPDATE,
        createTeamInfo(rendered),
        emptyList(),
      )
    sendPacket(viewer, wrapper)
  }

  fun rebindEntity(viewer: Player, teamName: String, playerName: String) {
    val wrapper =
      WrapperPlayServerTeams(
        teamName,
        WrapperPlayServerTeams.TeamMode.ADD_ENTITIES,
        null as WrapperPlayServerTeams.ScoreBoardTeamInfo?,
        listOf(playerName),
      )
    sendPacket(viewer, wrapper)
  }

  fun removeTeam(viewer: Player, teamName: String) {
    val wrapper =
      WrapperPlayServerTeams(
        teamName,
        WrapperPlayServerTeams.TeamMode.REMOVE,
        null as WrapperPlayServerTeams.ScoreBoardTeamInfo?,
        emptyList<String>(),
      )
    sendPacket(viewer, wrapper)
  }

  private fun createTeamInfo(rendered: RenderedTag): WrapperPlayServerTeams.ScoreBoardTeamInfo {
    return WrapperPlayServerTeams.ScoreBoardTeamInfo(
      Component.empty(),
      componentCache.component(rendered.prefix),
      componentCache.component(rendered.suffix),
      WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
      WrapperPlayServerTeams.CollisionRule.ALWAYS,
      NamedTextColor.WHITE,
      WrapperPlayServerTeams.OptionData.NONE,
    )
  }

  private fun sendPacket(viewer: Player, packet: PacketWrapper<*>) {
    PacketEvents.getAPI().playerManager.sendPacket(viewer, packet)
  }
}

internal class ViewBelowNamePacketBridge(private val componentCache: ViewComponentCache) {
  fun supportsFancyText(): Boolean {
    return PacketEvents.getAPI().serverManager.version.isNewerThanOrEquals(ServerVersion.V_1_20_3)
  }

  fun createObjective(viewer: Player, objectiveName: String, title: String, defaultText: String) {
    val createObjective =
      if (supportsFancyText()) {
        WrapperPlayServerScoreboardObjective(
          objectiveName,
          WrapperPlayServerScoreboardObjective.ObjectiveMode.CREATE,
          componentCache.component(title),
          WrapperPlayServerScoreboardObjective.RenderType.INTEGER,
          ScoreFormat.fixedScore(componentCache.component(defaultText)),
        )
      } else {
        WrapperPlayServerScoreboardObjective(
          objectiveName,
          WrapperPlayServerScoreboardObjective.ObjectiveMode.CREATE,
          componentCache.component(title),
          WrapperPlayServerScoreboardObjective.RenderType.INTEGER,
        )
      }
    val displayObjective =
      WrapperPlayServerDisplayScoreboard(BELOW_NAME_DISPLAY_SLOT, objectiveName)
    sendPacket(viewer, createObjective)
    sendPacket(viewer, displayObjective)
  }

  fun displayObjective(viewer: Player, objectiveName: String) {
    sendPacket(viewer, WrapperPlayServerDisplayScoreboard(BELOW_NAME_DISPLAY_SLOT, objectiveName))
  }

  fun updateEntry(
    viewer: Player,
    objectiveName: String,
    targetName: String,
    text: String,
    score: Int,
  ) {
    val wrapper = createUpdateScorePacket(objectiveName, targetName, text, score)
    sendPacket(viewer, wrapper)
  }

  fun removeEntry(viewer: Player, objectiveName: String, targetName: String) {
    val wrapper = createRemoveScorePacket(objectiveName, targetName)
    sendPacket(viewer, wrapper)
  }

  fun removeObjective(viewer: Player, objectiveName: String) {
    val wrapper =
      WrapperPlayServerScoreboardObjective(
        objectiveName,
        WrapperPlayServerScoreboardObjective.ObjectiveMode.REMOVE,
        Component.empty(),
        null,
      )
    sendPacket(viewer, wrapper)
  }

  private fun sendPacket(viewer: Player, packet: PacketWrapper<*>) {
    PacketEvents.getAPI().playerManager.sendPacket(viewer, packet)
  }

  private fun createUpdateScorePacket(
    objectiveName: String,
    targetName: String,
    text: String,
    score: Int,
  ): PacketWrapper<*> {
    return if (supportsFancyText()) {
      WrapperPlayServerUpdateScore(
        targetName,
        WrapperPlayServerUpdateScore.Action.CREATE_OR_UPDATE_ITEM,
        objectiveName,
        score,
        null,
        ScoreFormat.fixedScore(componentCache.component(text)),
      )
    } else {
      WrapperPlayServerUpdateScore(
        targetName,
        WrapperPlayServerUpdateScore.Action.CREATE_OR_UPDATE_ITEM,
        objectiveName,
        java.util.Optional.of(score),
      )
    }
  }

  private fun createRemoveScorePacket(objectiveName: String, targetName: String): PacketWrapper<*> {
    return if (supportsFancyText()) {
      WrapperPlayServerResetScore(targetName, objectiveName)
    } else {
      WrapperPlayServerUpdateScore(
        targetName,
        WrapperPlayServerUpdateScore.Action.REMOVE_ITEM,
        objectiveName,
        java.util.Optional.empty(),
      )
    }
  }
}

internal class ViewTagRenderer(private val playerDataManager: PlayerDataManager) {
  fun render(target: Player, state: TargetTeamState, config: ViewRuntimeConfig): RenderedTag {
    val slothTarget = playerDataManager.getPlayer(target)
    if (slothTarget == null) {
      val fallbackValues =
        mapOf(
          "prob" to config.fallbackProb,
          "buffer" to config.fallbackBuffer,
          "ping" to state.resolvePingDisplay(target.ping, config),
        )
      return RenderedTag(
        applyTemplate(config.prefixTemplate, fallbackValues),
        applyTemplate(config.suffixTemplate, fallbackValues),
        applyTemplate(config.belowTemplate, fallbackValues),
        ZERO_BELOW_SCORE,
      )
    }
    val aiCheck = slothTarget.checkManager.getCheck(AiCheck::class.java)

    val probabilityValue =
      if (aiCheck == null) {
        config.fallbackProb
      } else {
        formatDecimal(aiCheck.lastProbability * PERCENT_MULTIPLIER, config.probDecimals)
      }
    val belowScore =
      if (aiCheck == null) {
        ZERO_BELOW_SCORE
      } else {
        (aiCheck.lastProbability * PERCENT_MULTIPLIER).roundToInt().coerceAtLeast(ZERO_BELOW_SCORE)
      }

    val bufferValue =
      if (aiCheck == null) {
        config.fallbackBuffer
      } else {
        formatDecimal(aiCheck.buffer, config.bufferDecimals)
      }

    val values =
      mapOf(
        "prob" to probabilityValue,
        "buffer" to bufferValue,
        "ping" to state.resolvePingDisplay(target.ping, config),
      )

    return RenderedTag(
      applyTemplate(config.prefixTemplate, values),
      applyTemplate(config.suffixTemplate, values),
      applyTemplate(config.belowTemplate, values),
      belowScore,
    )
  }

  private fun applyTemplate(template: String, values: Map<String, String>): String {
    return renderViewTemplate(template, values)
  }

  private fun formatDecimal(value: Double, decimals: Int): String {
    val safeDecimals = decimals.coerceAtLeast(0)
    val normalized = if (abs(value) < DECIMAL_EPSILON) 0.0 else value
    return String.format(Locale.US, "%.${safeDecimals}f", normalized)
  }

  private companion object {
    const val ZERO_BELOW_SCORE = 0
    const val PERCENT_MULTIPLIER = 100.0
    const val DECIMAL_EPSILON = 0.0000001
  }
}

internal class ViewComponentCache(private val maxSize: Int = 256) {
  private val cache = ConcurrentHashMap<String, Component>()

  fun component(raw: String): Component {
    val cached = cache[raw]
    if (cached != null) {
      return cached
    }

    if (cache.size >= maxSize) {
      cache.clear()
    }

    val parsed = MessageUtil.deserializeRaw(raw)
    val existing = cache.putIfAbsent(raw, parsed)
    return existing ?: parsed
  }
}

internal data class ViewRuntimeConfig(
  val updateTicks: Long,
  val rebindCycles: Int,
  val resyncCycles: Int,
  val pingRefreshCycles: Int,
  val pingBucketMs: Int,
  val placement: ViewPlacement,
  val belowTitle: String,
  val fallbackProb: String,
  val fallbackBuffer: String,
  val probDecimals: Int,
  val bufferDecimals: Int,
  val prefixTemplate: String,
  val suffixTemplate: String,
  val belowTemplate: String,
  val defaultBelowText: String,
  val usesPing: Boolean,
) {
  companion object {
    fun from(config: ConfigView): ViewRuntimeConfig {
      val updateTicks = config.getLong("view.update", DEFAULT_UPDATE_TICKS).coerceAtLeast(1L)
      val rebindTicks = config.getLong("view.rebind-ticks", DEFAULT_REBIND_TICKS).coerceAtLeast(1L)
      val resyncTicks = config.getLong("view.resync-ticks", DEFAULT_RESYNC_TICKS).coerceAtLeast(1L)
      val pingRefreshTicks =
        config.getLong("view.ping-refresh-ticks", DEFAULT_PING_REFRESH_TICKS).coerceAtLeast(1L)
      val pingBucketMs =
        config.getInt("view.ping-bucket-ms", DEFAULT_PING_BUCKET_MS).coerceAtLeast(1)
      val prefixTemplate = config.getString("view.template.prefix", DEFAULT_PREFIX_TEMPLATE)
      val suffixTemplate = config.getString("view.template.suffix", DEFAULT_SUFFIX_TEMPLATE)
      val belowTemplate = config.getString("view.template.below", DEFAULT_BELOW_TEMPLATE)

      return ViewRuntimeConfig(
        updateTicks = updateTicks,
        rebindCycles = ticksToCycles(rebindTicks, updateTicks),
        resyncCycles = ticksToCycles(resyncTicks, updateTicks),
        pingRefreshCycles = ticksToCycles(pingRefreshTicks, updateTicks),
        pingBucketMs = pingBucketMs,
        placement = parseViewPlacement(config.getString("view.position", DEFAULT_VIEW_POSITION)),
        belowTitle = config.getString("view.template.below-title", DEFAULT_BELOW_TITLE),
        fallbackProb = config.getString("view.fallback.prob", DEFAULT_FALLBACK_PROB),
        fallbackBuffer = config.getString("view.fallback.buffer", DEFAULT_FALLBACK_BUFFER),
        probDecimals = config.getInt("view.format.prob-decimals", DEFAULT_PROB_DECIMALS),
        bufferDecimals = config.getInt("view.format.buffer-decimals", DEFAULT_BUFFER_DECIMALS),
        prefixTemplate = prefixTemplate,
        suffixTemplate = suffixTemplate,
        belowTemplate = belowTemplate,
        defaultBelowText =
          renderViewTemplate(
            belowTemplate,
            mapOf(
              "prob" to config.getString("view.fallback.prob", DEFAULT_FALLBACK_PROB),
              "buffer" to config.getString("view.fallback.buffer", DEFAULT_FALLBACK_BUFFER),
              "ping" to DEFAULT_FALLBACK_PING,
            ),
          ),
        usesPing =
          prefixTemplate.contains(PING_PLACEHOLDER) ||
            suffixTemplate.contains(PING_PLACEHOLDER) ||
            belowTemplate.contains(PING_PLACEHOLDER),
      )
    }

    private fun ticksToCycles(ticks: Long, updateTicks: Long): Int {
      return ((ticks + updateTicks - 1L) / updateTicks).coerceAtLeast(1L).toInt()
    }
  }
}

internal enum class ViewPlacement {
  ABOVE_NAME,
  BELOW_NAME,
}

internal fun parseViewPlacement(raw: String?): ViewPlacement {
  val normalized = raw?.trim().orEmpty()
  return when {
    normalized.equals("below", ignoreCase = true) -> ViewPlacement.BELOW_NAME
    normalized.equals("below_name", ignoreCase = true) -> ViewPlacement.BELOW_NAME
    else -> ViewPlacement.ABOVE_NAME
  }
}

private fun objectiveNameForViewer(viewerId: UUID): String {
  val compact = viewerId.toString().replace("-", "")
  return OBJECTIVE_PREFIX + compact.substring(0, OBJECTIVE_HASH_LENGTH)
}

private fun teamNameForView(uuid: UUID): String {
  val compact = uuid.toString().replace("-", "")
  return TEAM_PREFIX + compact.substring(0, TEAM_HASH_LENGTH)
}

private const val TEAM_PREFIX = "slv_"
private const val TEAM_HASH_LENGTH = 12
private const val OBJECTIVE_PREFIX = "svw_"
private const val OBJECTIVE_HASH_LENGTH = 12

internal const val DEFAULT_UPDATE_TICKS = 2L
internal const val DEFAULT_REBIND_TICKS = 100L
internal const val DEFAULT_RESYNC_TICKS = 100L
internal const val DEFAULT_PING_REFRESH_TICKS = 20L
internal const val DEFAULT_PING_BUCKET_MS = 10
internal const val BELOW_NAME_DISPLAY_SLOT = 2
internal const val DEFAULT_VIEW_POSITION = "BELOW_NAME"
internal const val DEFAULT_BELOW_TITLE = ""
internal const val DEFAULT_FALLBACK_PROB = "--"
internal const val DEFAULT_FALLBACK_BUFFER = "--"
internal const val DEFAULT_PROB_DECIMALS = 0
internal const val DEFAULT_BUFFER_DECIMALS = 2
internal const val DEFAULT_FALLBACK_PING = "--"
internal const val PING_PLACEHOLDER = "{ping}"
internal const val DEFAULT_PREFIX_TEMPLATE =
  "<dark_gray>[</dark_gray><white>{prob}%</white><dark_gray> • </dark_gray>" +
    "<yellow>{buffer}</yellow><dark_gray> • </dark_gray>" +
    "<aqua>{ping}ms</aqua><dark_gray>]</dark_gray> "
internal const val DEFAULT_SUFFIX_TEMPLATE = ""
internal const val DEFAULT_BELOW_TEMPLATE =
  "<dark_gray>[</dark_gray><white>{prob}%</white><dark_gray> • </dark_gray>" +
    "<yellow>{buffer}</yellow><dark_gray> • </dark_gray>" +
    "<aqua>{ping}ms</aqua><dark_gray>]</dark_gray>"

internal data class RenderedTag(
  val prefix: String,
  val suffix: String,
  val below: String,
  val belowScore: Int?,
)

private fun renderViewTemplate(template: String, values: Map<String, String>): String {
  var result = template
  for ((key, value) in values) {
    result = result.replace("{$key}", value)
  }
  return result
}
