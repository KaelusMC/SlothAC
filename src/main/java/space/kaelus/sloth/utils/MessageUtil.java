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
package space.kaelus.sloth.utils;

import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import space.kaelus.sloth.config.LocaleManager;

public class MessageUtil {

  private static final MiniMessage miniMessage = MiniMessage.miniMessage();
  private static LocaleManager localeManager;
  private static BukkitAudiences adventure;

  public static void init(LocaleManager localeManager, BukkitAudiences adventure) {
    MessageUtil.localeManager = localeManager;
    MessageUtil.adventure = adventure;
  }

  public static Component format(String message, String... placeholders) {
    String processedMessage =
        message.replace("<prefix>", localeManager.getRawMessage(Message.PREFIX));

    TagResolver.Builder resolverBuilder = TagResolver.builder();
    if (placeholders.length > 0) {
      if (placeholders.length % 2 != 0) {
        System.err.println("Invalid placeholders count for message: " + message);
      } else {
        for (int i = 0; i < placeholders.length; i += 2) {
          String key = placeholders[i];
          String value = placeholders[i + 1];

          resolverBuilder.resolver(Placeholder.component(key, Component.text(value)));
        }
      }
    }

    return miniMessage.deserialize(processedMessage, resolverBuilder.build());
  }

  public static void sendMessage(CommandSender sender, Message key, String... placeholders) {
    adventure.sender(sender).sendMessage(getMessage(key, placeholders));
  }

  public static void sendMessageList(CommandSender sender, Message key, String... placeholders) {
    getMessageList(key, placeholders).forEach(line -> adventure.sender(sender).sendMessage(line));
  }

  public static Component getMessage(Message key, String... placeholders) {
    String rawMessage = localeManager.getRawMessage(key);
    return format(rawMessage, placeholders);
  }

  public static Component getMessage(Message key, TagResolver resolver) {
    String rawMessage = localeManager.getRawMessage(key);
    String processedMessage =
        rawMessage.replace("<prefix>", localeManager.getRawMessage(Message.PREFIX));
    return miniMessage.deserialize(processedMessage, resolver);
  }

  public static List<Component> getMessageList(Message key, String... placeholders) {
    return localeManager.getRawMessageList(key).stream()
        .map(line -> format(line, placeholders))
        .collect(Collectors.toList());
  }
}
