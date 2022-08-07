package net.azisaba.interchat.api.guild;

import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.user.User;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GuildInvite {
    private final long guildId;
    private final UUID target;
    private final UUID actor;
    private final long expiresAt;

    public GuildInvite(long guildId, @NotNull UUID target, @NotNull UUID actor, long expiresAt) {
        this.guildId = guildId;
        this.target = target;
        this.actor = actor;
        this.expiresAt = expiresAt;
    }

    public static @NotNull GuildInvite createFromResultSet(@NotNull ResultSet rs) throws SQLException {
        long guildId = rs.getLong("guild_id");
        UUID target = UUID.fromString(rs.getString("target"));
        UUID actor = UUID.fromString(rs.getString("actor"));
        long expiresAt = rs.getLong("expires_at");
        return new GuildInvite(guildId, target, actor, expiresAt);
    }

    @Contract(pure = true)
    public long guildId() {
        return guildId;
    }

    @Contract(pure = true)
    @NotNull
    public UUID target() {
        return target;
    }

    @Contract(pure = true)
    @NotNull
    public UUID actor() {
        return actor;
    }

    @Contract(pure = true)
    public long expiresAt() {
        return expiresAt;
    }

    public @NotNull CompletableFuture<Guild> getGuild() {
        return InterChatProvider.get().getGuildManager().fetchGuildById(guildId);
    }

    public @NotNull CompletableFuture<User> getTargetUser() {
        return InterChatProvider.get().getUserManager().fetchUser(target);
    }

    public @NotNull CompletableFuture<User> getActorUser() {
        return InterChatProvider.get().getUserManager().fetchUser(actor);
    }

    public boolean isExpired() {
        return expiresAt < System.currentTimeMillis();
    }

    public @NotNull CompletableFuture<Void> delete() {
        return InterChatProvider.get().getGuildManager().deleteInvite(this);
    }
}
