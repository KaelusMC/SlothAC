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
import space.kaelus.sloth.checks.impl.ai.DataCollectorManager;
import space.kaelus.sloth.command.CommandManager;
import space.kaelus.sloth.config.ConfigManager;
import space.kaelus.sloth.config.LocaleManager;
import space.kaelus.sloth.database.DatabaseManager;
import space.kaelus.sloth.debug.DebugManager;
import space.kaelus.sloth.event.DamageEvent;
import space.kaelus.sloth.integration.WorldGuardManager;
import space.kaelus.sloth.packet.PacketListener;
import space.kaelus.sloth.player.PlayerDataManager;
import space.kaelus.sloth.server.AIServerProvider;
import space.kaelus.sloth.utils.MessageUtil;

public final class SlothAC extends JavaPlugin {
  private PlayerDataManager playerDataManager;
  private ConfigManager configManager;
  private LocaleManager localeManager;
  private AIServerProvider aiServerProvider;
  private WorldGuardManager worldGuardManager;
  private CommandManager commandManager;
  private AlertManager alertManager;
  private DatabaseManager databaseManager;
  private DataCollectorManager dataCollectorManager;
  @Getter private DebugManager debugManager;

  @Getter private BukkitAudiences adventure;

  @Override
  public void onLoad() {
    PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
    PacketEvents.getAPI().getSettings().checkForUpdates(false).bStats(true);
    PacketEvents.getAPI().load();
  }

  @Override
  public void onEnable() {
    this.adventure = BukkitAudiences.create(this);

    this.configManager = new ConfigManager(this);
    this.localeManager = new LocaleManager(this, configManager);
    this.debugManager = new DebugManager(this, configManager);

    MessageUtil.init(this.localeManager, this.adventure);

    this.databaseManager = new DatabaseManager(this, configManager);
    this.dataCollectorManager = new DataCollectorManager(this);
    this.worldGuardManager = new WorldGuardManager(this, configManager);
    this.alertManager = new AlertManager(this, configManager, localeManager, adventure);
    this.aiServerProvider = new AIServerProvider(this, configManager);

    this.playerDataManager =
        new PlayerDataManager(
            this,
            alertManager,
            dataCollectorManager,
            configManager,
            databaseManager,
            this.aiServerProvider,
            worldGuardManager);

    PacketEvents.getAPI()
        .getEventManager()
        .registerListener(new PacketListener(this.playerDataManager));
    PacketEvents.getAPI().init();

    this.commandManager =
        new CommandManager(
            this,
            alertManager,
            dataCollectorManager,
            databaseManager,
            configManager,
            localeManager,
            playerDataManager);

    getServer().getPluginManager().registerEvents(new DamageEvent(playerDataManager), this);
  }

  public void reloadPlugin() {
    configManager.reloadConfig();
    localeManager.reload();
    debugManager.reload();
    alertManager.reload();

    aiServerProvider.reload();

    if (playerDataManager != null) {
      playerDataManager.reloadAllPlayers();
    }
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
}
