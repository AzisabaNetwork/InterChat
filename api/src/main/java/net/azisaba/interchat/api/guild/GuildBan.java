package net.azisaba.interchat.api.guild;

import net.azisaba.interchat.api.InterChatProvider;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GuildBan {
    private final long guildId;
    private final UUID uuid;
    private final String reason;
    private final boolean reasonPublic;

    @Contract(pure = true)
    public GuildBan(long guildId, @NotNull UUID uuid, @Nullable String reason, boolean reasonPublic) {
        this.guildId = guildId;
        this.uuid = uuid;
        this.reason = reason;
        this.reasonPublic = reasonPublic;
    }



    @Contract("_ -> new")
    public static @NotNull GuildBan createByResultSet(@NotNull ResultSet rs) {
        try {
            long id = rs.getLong("guild_id");
            UUID uuid = UUID.fromString(rs.getString("uuid"));
            String reason = rs.getString("reason");
            boolean reasonPublic = rs.getBoolean("reason_public");
            return new GuildBan(id, uuid, reason, reasonPublic);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Contract(pure = true)
    public long guildId() {
        return guildId;
    }

    @Contract(pure = true)
    public @NotNull UUID uuid() {
        return uuid;
    }

    @Contract(pure = true)
    public @Nullable String reason() {
        return reason;
    }

    @Contract(pure = true)
    public boolean reasonPublic() {
        return reasonPublic;
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GuildBan)) return false;
        GuildBan guildBan = (GuildBan) o;
        return guildId == guildBan.guildId && reasonPublic == guildBan.reasonPublic && Objects.equals(uuid, guildBan.uuid) && Objects.equals(reason, guildBan.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(guildId, uuid, reason, reasonPublic);
    }

    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
        return "GuildBan{" +
                "guildId=" + guildId +
                ", uuid=" + uuid +
                ", reason='" + reason + '\'' +
                ", reasonPublic=" + reasonPublic +
                '}';
    }

    public @NotNull CompletableFuture<Void> delete() {
        return InterChatProvider.get().getGuildManager().deleteBan(this);
    }
}
