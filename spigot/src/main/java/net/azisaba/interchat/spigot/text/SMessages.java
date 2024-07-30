package net.azisaba.interchat.spigot.text;

import net.azisaba.interchat.api.text.Messages;
import net.azisaba.interchat.api.text.TranslatableKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class SMessages {
    public static void sendFormatted(@NotNull CommandSender source, @TranslatableKey @NotNull String key, Object @NotNull ... args) {
        source.sendMessage(format(source, key, args));
    }

    public static @NotNull String format(@NotNull CommandSender source, @TranslatableKey @NotNull String key, Object @NotNull ... args) {
        Locale locale = Locale.ENGLISH;
        if (source instanceof Player) {
            locale = Locale.forLanguageTag(((Player) source).getLocale().replaceAll("(.+)_.*", "$1"));
            if (locale.toLanguageTag().equals("und")) {
                locale = Locale.ENGLISH;
            }
        }
        String rawMessage = Messages.getInstance(locale).get(key);
        return String.format(Locale.ROOT, rawMessage, args);
    }
}
