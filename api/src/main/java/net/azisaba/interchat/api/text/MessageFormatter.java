package net.azisaba.interchat.api.text;

import net.azisaba.interchat.api.guild.Guild;
import net.azisaba.interchat.api.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class MessageFormatter {
    /**
     * @deprecated Use {@link #format(String, Guild, String, User, String, String, String)} instead
     */
    @Deprecated
    public static @NotNull String format(
            @NotNull String format,
            @NotNull Guild guild,
            @NotNull String server,
            @NotNull User sender,
            @NotNull String message,
            @Nullable String transliteratedMessage
    ) {
        return format(format, guild, server, sender, null, message, transliteratedMessage);
    }

    /**
     * Formats the message using arguments.
     * @param format the "format"
     * @param guild the guild
     * @param server the server the message was sent from
     * @param sender the sender
     * @param nickname the nickname of the sender
     * @param message the original message
     * @param transliteratedMessage the transliterated message ({@link Transliterator})
     * @return the formatted message
     */
    public static @NotNull String format(
            @NotNull String format,
            @NotNull Guild guild,
            @NotNull String server,
            @NotNull User sender,
            @Nullable String nickname,
            @NotNull String message,
            @Nullable String transliteratedMessage
    ) {
        // TODO: maybe support %prefix and %suffix
        String msg = message;
        String preReplace = "";
        String preReplaceB = ""; // with bracket
        if (transliteratedMessage != null) {
            msg = transliteratedMessage;
            preReplace = message;
            preReplaceB = "(" + message + ")";
        }
        return format.replace("%gname", guild.name())
                .replace("%server", server)
                .replace("%playername", sender.name())
                .replace("%username-n", Optional.ofNullable(nickname).orElse(sender.name()))
                .replace("%username", Optional.ofNullable(nickname).map(s -> "~" + s).orElse(sender.name()))
                .replace("%msg", msg)
                .replace("%prereplace-b", preReplaceB)
                .replace("%prereplace", preReplace)
                ;
    }
}
