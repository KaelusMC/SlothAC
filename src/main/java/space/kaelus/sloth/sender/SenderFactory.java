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
package space.kaelus.sloth.sender;

import java.util.UUID;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.SenderMapper;
import org.jetbrains.annotations.NotNull;

public class SenderFactory implements SenderMapper<CommandSender, Sender> {
  private final BukkitAudiences adventure;

  public SenderFactory(BukkitAudiences adventure) {
    this.adventure = adventure;
  }

  @Override
  public Sender map(@NotNull CommandSender base) {
    if (base instanceof Player player) {
      return new PlayerSender(player, adventure);
    }
    return new ConsoleSender(base, adventure);
  }

  @Override
  public CommandSender reverse(@NotNull Sender mapped) {
    return mapped.getNativeSender();
  }

  private static class PlayerSender implements Sender {
    private final Player player;
    private final BukkitAudiences adventure;

    PlayerSender(Player player, BukkitAudiences adventure) {
      this.player = player;
      this.adventure = adventure;
    }

    @Override
    public String getName() {
      return player.getName();
    }

    @Override
    public UUID getUniqueId() {
      return player.getUniqueId();
    }

    @Override
    public void sendMessage(String message) {
      player.sendMessage(message);
    }

    @Override
    public void sendMessage(Component message) {
      adventure.player(player).sendMessage(message);
    }

    @Override
    public boolean hasPermission(String permission) {
      return player.hasPermission(permission);
    }

    @Override
    public boolean isConsole() {
      return false;
    }

    @Override
    public boolean isPlayer() {
      return true;
    }

    @Override
    public CommandSender getNativeSender() {
      return player;
    }

    @Override
    public Player getPlayer() {
      return player;
    }
  }

  private static class ConsoleSender implements Sender {
    private final CommandSender sender;
    private final BukkitAudiences adventure;

    ConsoleSender(CommandSender sender, BukkitAudiences adventure) {
      this.sender = sender;
      this.adventure = adventure;
    }

    @Override
    public String getName() {
      return CONSOLE_NAME;
    }

    @Override
    public UUID getUniqueId() {
      return CONSOLE_UUID;
    }

    @Override
    public void sendMessage(String message) {
      sender.sendMessage(message);
    }

    @Override
    public void sendMessage(Component message) {
      adventure.sender(sender).sendMessage(message);
    }

    @Override
    public boolean hasPermission(String permission) {
      return sender.hasPermission(permission);
    }

    @Override
    public boolean isConsole() {
      return true;
    }

    @Override
    public boolean isPlayer() {
      return false;
    }

    @Override
    public CommandSender getNativeSender() {
      return sender;
    }

    @Override
    public Player getPlayer() {
      return null;
    }
  }
}
