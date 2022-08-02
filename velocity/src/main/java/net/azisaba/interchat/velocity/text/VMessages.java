package net.azisaba.interchat.velocity.text;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.azisaba.interchat.api.text.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class VMessages {
    private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER =
            LegacyComponentSerializer.builder()
                    .character('&')
                    .extractUrls()
                    .hexColors()
                    .build();

    public static void sendFormatted(@NotNull CommandSource source, @NotNull String key, Object @NotNull ... args) {
        source.sendMessage(LEGACY_COMPONENT_SERIALIZER.deserialize(format(source, key, args)));
    }

    public static String format(@NotNull CommandSource source, @NotNull String key, Object @NotNull ... args) {
        Locale locale = Locale.ENGLISH;
        if (source instanceof Player) {
            locale = ((Player) source).getEffectiveLocale();
        }
        String rawMessage = Messages.getInstance(locale).get(key);
        return String.format(Locale.ROOT, rawMessage, args);
    }

    public static @NotNull Component fromLegacyText(@NotNull String text) {
        return LEGACY_COMPONENT_SERIALIZER.deserialize(text);
    }
}
