/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2025 KaelusMC
 *
 * This file contains code derived from GrimAC.
 * The original authors of GrimAC are credited below.
 *
 * Copyright (c) 2021-2025 GrimAC, DefineOutside and contributors.
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
package space.kaelus.sloth.config;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import space.kaelus.sloth.SlothAC;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Getter
public class ConfigManager {
    private final SlothAC plugin;
    private FileConfiguration config;
    private FileConfiguration punishments;

    private boolean aiEnabled;
    private String aiServerUrl;
    private String aiApiKey;
    private int aiSequence;
    private int aiStep;
    private boolean aiDebug;
    private double aiFlag;
    private double aiResetOnFlag;
    private double aiBufferMultiplier;
    private double aiBufferDecrease;
    private boolean aiDamageReductionEnabled;
    private double aiDamageReductionProb;
    private double aiDamageReductionMultiplier;

    private boolean aiWorldGuardEnabled;
    private List<String> aiDisabledRegions;

    private List<Pattern> ignoredClientPatterns;
    private boolean disconnectBlacklistedForge;

    public ConfigManager(SlothAC plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void reloadConfig() {
        loadConfigs();
        plugin.getLocaleManager().reload();
    }

    private void loadConfigs() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        File punishmentsFile = new File(plugin.getDataFolder(), "punishments.yml");
        if (!punishmentsFile.exists()) {
            plugin.saveResource("punishments.yml", false);
        }
        this.punishments = YamlConfiguration.loadConfiguration(punishmentsFile);

        loadValues();
    }

    private void loadValues() {
        aiEnabled = config.getBoolean("ai.enabled", false);
        aiServerUrl = config.getString("ai.server", "");
        aiApiKey = config.getString("ai.api-key", "API-KEY");
        aiSequence = config.getInt("ai.sequence", 40);
        aiStep = config.getInt("ai.step", 10);
        aiDebug = config.getBoolean("ai.debug", false);

        aiFlag = config.getDouble("ai.buffer-settings.flag", 50.0);
        aiResetOnFlag = config.getDouble("ai.buffer-settings.reset-on-flag", 25.0);
        aiBufferMultiplier = config.getDouble("ai.buffer-settings.multiplier", 100.0);
        aiBufferDecrease = config.getDouble("ai.buffer-settings.decrease", 0.25);

        aiDamageReductionEnabled = config.getBoolean("ai.damage-reduction.enabled", true);
        aiDamageReductionProb = config.getDouble("ai.damage-reduction.prob", 0.9);
        aiDamageReductionMultiplier = config.getDouble("ai.damage-reduction.multiplier", 1.0);

        aiWorldGuardEnabled = config.getBoolean("ai.worldguard.enabled", true);
        aiDisabledRegions = config.getStringList("ai.worldguard.disabled-regions")
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        ignoredClientPatterns = new ArrayList<>();
        for (String pattern : config.getStringList("client-brand.ignored-clients")) {
            try {
                ignoredClientPatterns.add(Pattern.compile(pattern));
            } catch (PatternSyntaxException e) {
                plugin.getLogger().warning("[ClientBrand] Invalid regex pattern in config: " + pattern);
            }
        }

        disconnectBlacklistedForge = config.getBoolean("client-brand.disconnect-blacklisted-forge-versions", true);
    }

    public boolean isClientIgnored(String brand) {
        for (Pattern pattern : ignoredClientPatterns) {
            if (pattern.matcher(brand).find()) {
                return true;
            }
        }
        return false;
    }
}