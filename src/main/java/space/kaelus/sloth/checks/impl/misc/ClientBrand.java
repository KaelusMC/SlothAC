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
package space.kaelus.sloth.checks.impl.misc;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import java.nio.charset.StandardCharsets;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import space.kaelus.sloth.alert.AlertManager;
import space.kaelus.sloth.alert.AlertType;
import space.kaelus.sloth.checks.AbstractCheck;
import space.kaelus.sloth.checks.CheckData;
import space.kaelus.sloth.checks.CheckFactory;
import space.kaelus.sloth.checks.type.PacketCheck;
import space.kaelus.sloth.config.ConfigManager;
import space.kaelus.sloth.player.SlothPlayer;
import space.kaelus.sloth.utils.ChatUtil;
import space.kaelus.sloth.utils.Message;
import space.kaelus.sloth.utils.MessageUtil;

@CheckData(name = "ClientBrand_Internal")
public class ClientBrand extends AbstractCheck implements PacketCheck {

  private static final String CHANNEL =
      PacketEvents.getAPI()
              .getServerManager()
              .getVersion()
              .isNewerThanOrEquals(ServerVersion.V_1_13)
          ? "minecraft:brand"
          : "MC|Brand";

  private final ConfigManager configManager;
  private final AlertManager alertManager;

  @Getter private String brand = "vanilla";
  private boolean hasBrand = false;

  @AssistedInject
  public ClientBrand(
      @Assisted SlothPlayer player, ConfigManager configManager, AlertManager alertManager) {
    super(player);
    this.configManager = configManager;
    this.alertManager = alertManager;
  }

  @AssistedFactory
  public interface Factory extends CheckFactory {
    @Override
    ClientBrand create(SlothPlayer slothPlayer);
  }

  @Override
  public void onPacketReceive(final PacketReceiveEvent event) {
    if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
      WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
      handle(packet.getChannelName(), packet.getData());
    } else if (event.getPacketType() == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
      WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
      handle(packet.getChannelName(), packet.getData());
    }
  }

  private void handle(String channel, byte[] data) {
    if (!channel.equals(ClientBrand.CHANNEL) || hasBrand) {
      return;
    }

    hasBrand = true;

    if (data.length > 64 || data.length == 0) {
      brand = "invalid (" + data.length + " bytes)";
    } else {
      byte[] brandBytes = new byte[data.length - 1];
      System.arraycopy(data, 1, brandBytes, 0, brandBytes.length);

      brand = new String(brandBytes, StandardCharsets.UTF_8).replace(" (Velocity)", "");
      brand = ChatUtil.stripColor(brand);
    }

    slothPlayer.setBrand(brand);

    if (!configManager.isClientIgnored(brand)) {
      Component component =
          MessageUtil.getMessage(
              Message.BRAND_NOTIFICATION,
              "player",
              slothPlayer.getPlayer().getName(),
              "brand",
              brand);
      alertManager.send(component, AlertType.BRAND);
    }

    // https://github.com/MinecraftForge/MinecraftForge/issues/9309
    final boolean hasReachExploit =
        brand.contains("forge")
            && slothPlayer.getUser().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_18_2)
            && slothPlayer.getUser().getClientVersion().isOlderThan(ClientVersion.V_1_19_4);

    if (hasReachExploit && configManager.isDisconnectBlacklistedForge()) {
      slothPlayer.disconnect(MessageUtil.getMessage(Message.BRAND_DISCONNECT_FORGE));
    }
  }
}
