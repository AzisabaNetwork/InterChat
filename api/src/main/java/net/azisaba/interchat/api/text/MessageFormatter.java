package net.azisaba.interchat.api.text;

import net.azisaba.interchat.api.guild.Guild;
import net.azisaba.interchat.api.user.User;
import org.jetbrains.annotations.NotNull;

public final class MessageFormatter {
    public static @NotNull String format(
            @NotNull String format,
            @NotNull Guild guild,
            @NotNull String server,
            @NotNull User sender,
            @NotNull String message
    ) {
        // TODO: maybe support %prefix and %suffix
        return format.replace("%gname", guild.name())
                .replace("%server", server)
                .replace("%username", sender.name())
                .replace("%msg", message);
    }
}
