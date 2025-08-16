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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import space.kaelus.sloth.SlothAC;
import space.kaelus.sloth.config.LocaleManager;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MessageUtil {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final Pattern HEX_PATTERN = Pattern.compile("([&§]#[A-Fa-f0-9]{6})|([&§]x([&§][A-Fa-f0-9]){6})");

    private static LocaleManager getLocaleManager() {
        return SlothAC.getInstance().getLocaleManager();
    }

    public static Component format(String message, String... placeholders) {
        String processedMessage = message.replace("%prefix%", getLocaleManager().getRawMessage(Message.PREFIX));

        TagResolver.Builder resolverBuilder = TagResolver.builder();
        if (placeholders.length > 0) {
            if (placeholders.length % 2 != 0) {
                throw new IllegalArgumentException("Placeholders must be key-value pairs.");
            }
            for (int i = 0; i < placeholders.length; i += 2) {
                String key = placeholders[i];
                String value = placeholders[i + 1];
                processedMessage = processedMessage.replace("%" + key + "%", "<" + key + ">");
                resolverBuilder.resolver(Placeholder.component(key, Component.text(value)));
            }
        }

        Matcher matcher = HEX_PATTERN.matcher(processedMessage);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(0).replaceAll("[&§#x]", "");
            matcher.appendReplacement(sb, "<#" + hex + ">");
        }
        matcher.appendTail(sb);
        processedMessage = sb.toString();

        processedMessage = ChatUtil.translateAlternateColorCodes('&', processedMessage);

        processedMessage = processedMessage
                .replace("§0", "<!b><!i><!u><!st><!obf><black>")
                .replace("§1", "<!b><!i><!u><!st><!obf><dark_blue>")
                .replace("§2", "<!b><!i><!u><!st><!obf><dark_green>")
                .replace("§3", "<!b><!i><!u><!st><!obf><dark_aqua>")
                .replace("§4", "<!b><!i><!u><!st><!obf><dark_red>")
                .replace("§5", "<!b><!i><!u><!st><!obf><dark_purple>")
                .replace("§6", "<!b><!i><!u><!st><!obf><gold>")
                .replace("§7", "<!b><!i><!u><!st><!obf><gray>")
                .replace("§8", "<!b><!i><!u><!st><!obf><dark_gray>")
                .replace("§9", "<!b><!i><!u><!st><!obf><blue>")
                .replace("§a", "<!b><!i><!u><!st><!obf><green>")
                .replace("§b", "<!b><!i><!u><!st><!obf><aqua>")
                .replace("§c", "<!b><!i><!u><!st><!obf><red>")
                .replace("§d", "<!b><!i><!u><!st><!obf><light_purple>")
                .replace("§e", "<!b><!i><!u><!st><!obf><yellow>")
                .replace("§f", "<!b><!i><!u><!st><!obf><white>")
                .replace("§k", "<obfuscated>")
                .replace("§l", "<bold>")
                .replace("§m", "<strikethrough>")
                .replace("§n", "<underlined>")
                .replace("§o", "<italic>")
                .replace("§r", "<reset>");

        TagResolver tagResolver = resolverBuilder.build();
        return miniMessage.deserialize(processedMessage, tagResolver);
    }

    public static void sendMessage(CommandSender sender, Message key, String... placeholders) {
        SlothAC.getInstance().getAdventure().sender(sender).sendMessage(getMessage(key, placeholders));
    }

    public static void sendMessageList(CommandSender sender, Message key, String... placeholders) {
        getMessageList(key, placeholders).forEach(line -> SlothAC.getInstance().getAdventure().sender(sender).sendMessage(line));
    }

    public static Component getMessage(Message key, String... placeholders) {
        String rawMessage = getLocaleManager().getRawMessage(key);
        return format(rawMessage, placeholders);
    }

    public static List<Component> getMessageList(Message key, String... placeholders) {
        return getLocaleManager().getRawMessageList(key).stream()
                .map(line -> format(line, placeholders))
                .collect(Collectors.toList());
    }
}