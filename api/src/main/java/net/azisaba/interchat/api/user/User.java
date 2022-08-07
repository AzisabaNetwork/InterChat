package net.azisaba.interchat.api.user;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class User {
    private final UUID id;
    private final String name;
    @Range(from = -1, to = Long.MAX_VALUE)
    private final long selectedGuild;
    @Range(from = -1, to = Long.MAX_VALUE)
    private final long focusedGuild;
    private final boolean acceptingInvites;

    public User(@NotNull UUID id, @NotNull String name, long selectedGuild, long focusedGuild, boolean acceptingInvites) {
        this.id = id;
        this.name = name;
        this.selectedGuild = selectedGuild;
        this.focusedGuild = focusedGuild;
        this.acceptingInvites = acceptingInvites;
    }

    @Contract(pure = true)
    public static @NotNull User createByResultSet(@NotNull ResultSet rs) {
        try {
            UUID id = UUID.fromString(rs.getString("id"));
            String name = rs.getString("name");
            long selectedGuild = rs.getLong("selected_guild");
            long focusedGuild = rs.getLong("focused_guild");
            boolean acceptingInvites = rs.getBoolean("accepting_invites");
            return new User(id, name, selectedGuild, focusedGuild, acceptingInvites);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Contract(pure = true)
    @NotNull
    public UUID id() {
        return id;
    }

    @Contract(pure = true)
    @NotNull
    public String name() {
        return name;
    }

    @Contract(pure = true)
    @Range(from = -1, to = Long.MAX_VALUE)
    public long selectedGuild() {
        return selectedGuild;
    }

    @Contract(pure = true)
    @Range(from = -1, to = Long.MAX_VALUE)
    public long focusedGuild() {
        return focusedGuild;
    }

    @Contract(pure = true)
    public boolean acceptingInvites() {
        return acceptingInvites;
    }
}
