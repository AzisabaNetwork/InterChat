package net.azisaba.interchat.api.guild;

import net.azisaba.interchat.api.InterChatProvider;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GuildMember {
    private final long guildId;
    private final UUID uuid;
    private final GuildRole role;

    public GuildMember(long guildId, @NotNull UUID uuid, @NotNull GuildRole role) {
        this.guildId = guildId;
        this.uuid = uuid;
        this.role = role;
    }

    @Contract("_ -> new")
    public static @NotNull GuildMember createByResultSet(ResultSet rs) {
        try {
            long guildId = rs.getLong("guild_id");
            UUID uuid = UUID.fromString(rs.getString("uuid"));
            GuildRole role = GuildRole.valueOf(rs.getString("role"));
            return new GuildMember(guildId, uuid, role);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Contract(pure = true)
    public long guildId() {
        return guildId;
    }

    @Contract(pure = true)
    public UUID uuid() {
        return uuid;
    }

    @Contract(pure = true)
    public GuildRole role() {
        return role;
    }

    @Contract(pure = true)
    public @NotNull CompletableFuture<Guild> getGuild() {
        return InterChatProvider.get().getGuildManager().fetchGuildById(guildId);
    }
}
