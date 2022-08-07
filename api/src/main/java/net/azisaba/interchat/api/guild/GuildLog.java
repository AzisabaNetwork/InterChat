package net.azisaba.interchat.api.guild;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class GuildLog {
    private final long id;
    private final long guildId;
    private final UUID actor;
    private final String actorName;
    private final long time;
    private final String description;

    public GuildLog(long id, long guildId, UUID actor, String actorName, long time, String description) {
        this.id = id;
        this.guildId = guildId;
        this.actor = actor;
        this.actorName = actorName;
        this.time = time;
        this.description = description;
    }

    @Contract("_ -> new")
    public static @NotNull GuildLog createFromResultSet(@NotNull ResultSet rs) {
        try {
            long id = rs.getLong(1);
            long guildId = rs.getLong(2);
            UUID actor = UUID.fromString(rs.getString(3));
            String actorName = rs.getString(4);
            long time = rs.getLong(5);
            String description = rs.getString(6);
            return new GuildLog(id, guildId, actor, actorName, time, description);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long id() {
        return id;
    }

    public long guildId() {
        return guildId;
    }

    @NotNull
    public UUID actor() {
        return actor;
    }

    @NotNull
    public String actorName() {
        return actorName;
    }

    public long time() {
        return time;
    }

    @NotNull
    public String description() {
        return description;
    }
}
