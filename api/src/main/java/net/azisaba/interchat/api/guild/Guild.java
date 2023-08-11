package net.azisaba.interchat.api.guild;

import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.user.User;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class Guild {
    private final long id;
    private final String name;
    private final String format;
    private final int capacity;
    private final boolean deleted;
    private final boolean open;

    @Contract(pure = true)
    public Guild(long id, @NotNull String name, @NotNull String format, int capacity, boolean deleted, boolean open) {
        this.id = id;
        this.name = name;
        this.format = format;
        this.capacity = capacity;
        this.deleted = deleted;
        this.open = open;
    }

    @Contract("_ -> new")
    public static @NotNull Guild createByResultSet(@NotNull ResultSet rs) {
        try {
            long id = rs.getLong("id");
            String name = rs.getString("name");
            String format = rs.getString("format");
            int capacity = rs.getInt("capacity");
            boolean deleted = rs.getBoolean("deleted");
            boolean open = rs.getBoolean("open");
            return new Guild(id, name, format, capacity, deleted, open);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Contract(pure = true)
    public long id() {
        return id;
    }

    @Contract(pure = true)
    @NotNull
    public String name() {
        return name;
    }

    @Contract(pure = true)
    @NotNull
    public String format() {
        return format;
    }

    @Contract(pure = true)
    public int capacity() {
        return capacity;
    }

    @Contract(pure = true)
    public boolean deleted() {
        return deleted;
    }

    @Contract(pure = true)
    public boolean open() {
        return open;
    }

    @Contract(pure = true)
    public @NotNull CompletableFuture<List<GuildMember>> getMembers() {
        return InterChatProvider.get().getGuildManager().getMembers(this);
    }

    @Contract(pure = true)
    public @NotNull CompletableFuture<GuildMember> getMember(@NotNull UUID uuid) {
        return InterChatProvider.get().getGuildManager().getMember(this, uuid);
    }

    @Contract(pure = true)
    public @NotNull CompletableFuture<GuildMember> getMember(@NotNull User user) {
        return InterChatProvider.get().getGuildManager().getMember(this, user);
    }

    @Contract(pure = true)
    public @NotNull CompletableFuture<Collection<GuildBan>> getBans() {
        return InterChatProvider.get().getGuildManager().getBans(this);
    }

    @Contract(pure = true)
    public @NotNull CompletableFuture<Optional<GuildBan>> getBan(@NotNull UUID uuid) {
        return InterChatProvider.get().getGuildManager().getBan(this, uuid);
    }

    @Contract(pure = true)
    public @NotNull CompletableFuture<Optional<GuildBan>> getBan(@NotNull User user) {
        return InterChatProvider.get().getGuildManager().getBan(this, user);
    }
}
