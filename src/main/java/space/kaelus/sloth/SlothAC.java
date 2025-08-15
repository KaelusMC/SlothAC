/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2025 KaelusMC
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
package space.kaelus.sloth;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.java.JavaPlugin;
import space.kaelus.sloth.alert.AlertManager;
import space.kaelus.sloth.command.CommandManager;
import space.kaelus.sloth.config.ConfigManager;
import space.kaelus.sloth.config.LocaleManager;
import space.kaelus.sloth.database.DatabaseManager;
import space.kaelus.sloth.event.DamageEvent;
import space.kaelus.sloth.integration.WorldGuardManager;
import space.kaelus.sloth.packet.PacketListener;
import space.kaelus.sloth.player.PlayerDataManager;
import space.kaelus.sloth.server.AIServer;

@Getter
public final class SlothAC extends JavaPlugin {
    @Getter
    private static SlothAC instance;
    private PlayerDataManager playerDataManager;
    private ConfigManager configManager;
    private LocaleManager localeManager;
    private AIServer aiServer;
    private WorldGuardManager worldGuardManager;
    private CommandManager commandManager;
    private AlertManager alertManager;
    private DatabaseManager databaseManager;
    private BukkitAudiences adventure;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings()
                .checkForUpdates(false)
                .bStats(true);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        instance = this;

        this.adventure = BukkitAudiences.create(this);

        this.configManager = new ConfigManager(this);
        this.localeManager = new LocaleManager(this);
        this.alertManager = new AlertManager();
        this.databaseManager = new DatabaseManager();
        this.playerDataManager = new PlayerDataManager();
        this.worldGuardManager = new WorldGuardManager();
        loadAI();

        PacketEvents.getAPI().getEventManager().registerListener(new PacketListener(this.playerDataManager));
        PacketEvents.getAPI().init();

        this.commandManager = new CommandManager(this);
        getServer().getPluginManager().registerEvents(new DamageEvent(), this);
    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        if (PacketEvents.getAPI().isInitialized()) {
            PacketEvents.getAPI().terminate();
        }
    }

    public void loadAI() {
        if (configManager.isAiEnabled()) {
            String url = configManager.getAiServerUrl();
            String key = configManager.getAiApiKey();

            if (url == null || url.isEmpty() || key == null || key.equals("API-KEY")) {
                getLogger().warning("[AICheck] AI is enabled but not configured.");
                this.aiServer = null;
            } else {
                this.aiServer = new AIServer(url, key);
                getLogger().info("[AICheck] AI Check enabled.");
            }
        } else {
            this.aiServer = null;
            getLogger().info("[AICheck] AI Check disabled.");
        }
    }
}