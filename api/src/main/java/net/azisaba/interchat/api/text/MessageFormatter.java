package net.azisaba.interchat.api.text;

import net.azisaba.interchat.api.guild.Guild;
import net.azisaba.interchat.api.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MessageFormatter {
    public static @NotNull String format(
            @NotNull String format,
            @NotNull Guild guild,
            @NotNull String server,
            @NotNull User sender,
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
                .replace("%username", sender.name())
                .replace("%msg", msg)
                .replace("%prereplace-b", preReplaceB)
                .replace("%prereplace", preReplace)
                ;
    }
}
