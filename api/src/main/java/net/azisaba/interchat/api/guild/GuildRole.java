package net.azisaba.interchat.api.guild;

import net.azisaba.interchat.api.text.TranslatableKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum GuildRole {
    OWNER("guild.roles.owner"),
    MODERATOR("guild.roles.moderator"),
    MEMBER("guild.roles.member"),
    ;

    private final String key;

    GuildRole(@TranslatableKey @NotNull String key) {
        this.key = key;
    }

    @Contract(pure = true)
    @NotNull
    public String getKey() {
        return key;
    }
}
