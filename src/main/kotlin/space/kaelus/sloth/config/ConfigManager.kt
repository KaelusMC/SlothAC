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
package space.kaelus.sloth.config

import java.io.File
import java.util.EnumSet
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.debug.DebugCategory

class ConfigManager(private val plugin: SlothAC) {
  var config: ConfigView = ConfigView(CommentedConfigurationNode.root())
    private set

  var punishments: ConfigView = ConfigView(CommentedConfigurationNode.root())
    private set

  var monitorConfig: ConfigView = ConfigView(CommentedConfigurationNode.root())
    private set

  private var aiEnabled = false
  var aiServerUrl: String = ""
    private set

  var aiApiKey: String = ""
    private set

  var aiSequence: Int = 0
  var aiStep: Int = 0
    private set

  var aiFlag: Double = 0.0
    private set

  var aiResetOnFlag: Double = 0.0
    private set

  var aiBufferMultiplier: Double = 0.0
    private set

  var aiBufferDecrease: Double = 0.0
    private set

  private var aiDamageReductionEnabled = false
  var aiDamageReductionProb: Double = 0.0
    private set

  var aiDamageReductionMultiplier: Double = 0.0
    private set

  private var aiWorldGuardEnabled = false
  var aiDisabledRegions: Map<String, List<String>> = emptyMap()
    private set

  private var ignoredClientPatterns: List<Pattern> = emptyList()
  private var disconnectBlacklistedForge = false

  var suspiciousAlertsBuffer: Double = 0.0
    private set

  private var debugEnabled = false
  var enabledDebugCategories: Set<DebugCategory> = emptySet()
    private set

  init {
    loadConfigs()
  }

  fun reloadConfig() {
    loadConfigs()
  }

  fun isAiEnabled(): Boolean = aiEnabled

  fun isAiDamageReductionEnabled(): Boolean = aiDamageReductionEnabled

  fun isAiWorldGuardEnabled(): Boolean = aiWorldGuardEnabled

  fun isDisconnectBlacklistedForge(): Boolean = disconnectBlacklistedForge

  fun isDebugEnabled(): Boolean = debugEnabled

  private fun loadConfigs() {
    if (!plugin.dataFolder.exists()) {
      plugin.dataFolder.mkdirs()
    }

    config = loadConfig("config.yml")
    punishments = loadConfig("punishments.yml")
    monitorConfig = loadConfig("monitor.yml")

    loadValues()
  }

  private fun loadConfig(fileName: String): ConfigView {
    val file = File(plugin.dataFolder, fileName)
    if (!file.exists()) {
      plugin.saveResource(fileName, false)
    }
    return try {
      val loader = YamlConfigurationLoader.builder().path(file.toPath()).build()
      val node = loader.load()
      ConfigView(node)
    } catch (e: Exception) {
      plugin.logger.severe("Failed to load $fileName: ${e.message}")
      ConfigView(CommentedConfigurationNode.root())
    }
  }

  private fun loadValues() {
    aiEnabled = config.getBoolean("ai.enabled", false)
    aiServerUrl = config.getString("ai.server", "")
    aiApiKey = config.getString("ai.api-key", "API-KEY")
    aiSequence = config.getInt("ai.sequence", 40)
    aiStep = config.getInt("ai.step", 10)

    aiFlag = config.getDouble("ai.buffer.flag", 50.0)
    aiResetOnFlag = config.getDouble("ai.buffer.reset-on-flag", 25.0)
    aiBufferMultiplier = config.getDouble("ai.buffer.multiplier", 100.0)
    aiBufferDecrease = config.getDouble("ai.buffer.decrease", 0.25)

    aiDamageReductionEnabled = config.getBoolean("ai.damage-reduction.enabled", true)
    aiDamageReductionProb = config.getDouble("ai.damage-reduction.prob", 0.9)
    aiDamageReductionMultiplier = config.getDouble("ai.damage-reduction.multiplier", 1.0)

    aiWorldGuardEnabled = config.getBoolean("ai.worldguard.enabled", true)
    aiDisabledRegions = loadDisabledRegions()

    val ignoredPatterns = ArrayList<Pattern>()
    for (pattern in config.getStringList("client-brand.ignored-clients")) {
      try {
        ignoredPatterns.add(Pattern.compile(pattern))
      } catch (e: PatternSyntaxException) {
        plugin.logger.warning("[ClientBrand] Invalid regex pattern in config: $pattern")
      }
    }
    ignoredClientPatterns = ignoredPatterns

    disconnectBlacklistedForge =
      config.getBoolean("client-brand.disconnect-blacklisted-forge-versions", true)

    suspiciousAlertsBuffer = config.getDouble("suspicious.alerts.buffer", 25.0)

    debugEnabled = config.getBoolean("debug.enabled", false)
    val enabledCategories = EnumSet.noneOf(DebugCategory::class.java)
    for (category in DebugCategory.values()) {
      if (config.getBoolean("debug.categories.${category.configKey}", false)) {
        enabledCategories.add(category)
      }
    }
    enabledDebugCategories = enabledCategories
  }

  private fun loadDisabledRegions(): Map<String, List<String>> {
    val mapRegions = config.getStringListMap("ai.worldguard.disabled-regions")
    if (mapRegions.isNotEmpty()) {
      return mapRegions
        .mapKeys { it.key.lowercase() }
        .mapValues { entry -> entry.value.map { it.lowercase() } }
    }

    return parseLegacyDisabledRegions()
  }

  private fun parseLegacyDisabledRegions(): Map<String, List<String>> {
    val legacyList = config.getStringList("ai.worldguard.disabled-regions")
    if (legacyList.isEmpty()) return emptyMap()

    plugin.logger.warning(
      "[Config] ai.worldguard.disabled-regions uses deprecated " +
        "region:world format. Please migrate to the new map format."
    )
    val result = mutableMapOf<String, MutableList<String>>()
    for (entry in legacyList) {
      val lower = entry.lowercase()
      if (lower.contains(":")) {
        val parts = lower.split(":", limit = 2)
        val regionName = parts[0]
        val worldName = parts[1]
        result.getOrPut(worldName) { mutableListOf() }.add(regionName)
      } else {
        result.getOrPut("*") { mutableListOf() }.add(lower)
      }
    }
    return result
  }

  fun isClientIgnored(brand: String): Boolean {
    for (pattern in ignoredClientPatterns) {
      if (pattern.matcher(brand).find()) {
        return true
      }
    }
    return false
  }
}
