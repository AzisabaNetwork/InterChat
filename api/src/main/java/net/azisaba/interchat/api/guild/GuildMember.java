package net.azisaba.interchat.api.guild;

import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.user.User;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GuildMember {
    private final long guildId;
    private final UUID uuid;
    private final GuildRole role;
    @Nullable
    private final String nickname;

    public GuildMember(long guildId, @NotNull UUID uuid, @NotNull GuildRole role) {
        this(guildId, uuid, role, null);
    }

    public GuildMember(long guildId, @NotNull UUID uuid, @NotNull GuildRole role, @Nullable String nickname) {
        this.guildId = guildId;
        this.uuid = uuid;
        this.role = role;
        this.nickname = nickname;
    }

    @Contract("_ -> new")
    public static @NotNull GuildMember createByResultSet(ResultSet rs) {
        try {
            long guildId = rs.getLong("guild_id");
            UUID uuid = UUID.fromString(rs.getString("uuid"));
            GuildRole role = GuildRole.valueOf(rs.getString("role"));
            String nickname = rs.getString("nickname");
            return new GuildMember(guildId, uuid, role, nickname);
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
    public @Nullable String nickname() {
        return nickname;
    }

    @Contract(pure = true)
    public @NotNull CompletableFuture<User> getUser() {
        return InterChatProvider.get().getUserManager().fetchUser(uuid);
    }

    @Contract(pure = true)
    public @NotNull CompletableFuture<Guild> getGuild() {
        return InterChatProvider.get().getGuildManager().fetchGuildById(guildId);
    }

    public @NotNull CompletableFuture<Void> delete() {
        return InterChatProvider.get().getGuildManager().removeMember(guildId, uuid);
    }

    public @NotNull CompletableFuture<Void> update(@NotNull GuildRole role) {
        return InterChatProvider.get().getGuildManager().updateMemberRole(guildId, uuid, role);
    }

    public @NotNull CompletableFuture<Void> update() {
        return InterChatProvider.get().getGuildManager().updateMember(this);
    }
}
