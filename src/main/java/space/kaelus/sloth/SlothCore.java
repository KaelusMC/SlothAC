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
import javax.inject.Inject;
import javax.inject.Singleton;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import space.kaelus.sloth.alert.AlertManager;
import space.kaelus.sloth.command.CommandManager;
import space.kaelus.sloth.config.ConfigManager;
import space.kaelus.sloth.config.LocaleManager;
import space.kaelus.sloth.database.DatabaseManager;
import space.kaelus.sloth.debug.DebugManager;
import space.kaelus.sloth.event.DamageEvent;
import space.kaelus.sloth.packet.PacketListener;
import space.kaelus.sloth.player.PlayerDataManager;
import space.kaelus.sloth.server.AIServerProvider;
import space.kaelus.sloth.utils.MessageUtil;

@Singleton
public final class SlothCore {

  private final SlothAC plugin;
  private final PlayerDataManager playerDataManager;
  private final ConfigManager configManager;
  private final LocaleManager localeManager;
  private final AIServerProvider aiServerProvider;
  private final CommandManager commandManager;
  private final AlertManager alertManager;
  private final DatabaseManager databaseManager;
  private final DebugManager debugManager;
  private final PacketListener packetListener;
  private final DamageEvent damageEvent;
  private final BukkitAudiences adventure;

  @Inject
  public SlothCore(
      SlothAC plugin,
      PlayerDataManager playerDataManager,
      ConfigManager configManager,
      LocaleManager localeManager,
      AIServerProvider aiServerProvider,
      CommandManager commandManager,
      AlertManager alertManager,
      DatabaseManager databaseManager,
      DebugManager debugManager,
      PacketListener packetListener,
      DamageEvent damageEvent,
      BukkitAudiences adventure) {
    this.plugin = plugin;
    this.playerDataManager = playerDataManager;
    this.configManager = configManager;
    this.localeManager = localeManager;
    this.aiServerProvider = aiServerProvider;
    this.commandManager = commandManager;
    this.alertManager = alertManager;
    this.databaseManager = databaseManager;
    this.debugManager = debugManager;
    this.packetListener = packetListener;
    this.damageEvent = damageEvent;
    this.adventure = adventure;
  }

  public void enable() {
    commandManager.registerCommands();

    MessageUtil.init(this.localeManager, this.adventure);

    PacketEvents.getAPI().getEventManager().registerListener(packetListener);
    PacketEvents.getAPI().init();

    plugin.getServer().getPluginManager().registerEvents(damageEvent, plugin);
  }

  public void disable() {
    adventure.close();
    databaseManager.shutdown();
    if (PacketEvents.getAPI().isInitialized()) {
      PacketEvents.getAPI().terminate();
    }
  }

  public void reload() {
    configManager.reloadConfig();
    localeManager.reload();
    debugManager.reload();
    alertManager.reload();
    aiServerProvider.reload();
    playerDataManager.reloadAllPlayers();
  }
}
