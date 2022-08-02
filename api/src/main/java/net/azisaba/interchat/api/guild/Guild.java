package net.azisaba.interchat.api.guild;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class Guild {
    private final long id;
    private final String name;
    private final String format;
    private final int capacity;
    private final boolean deleted;

    @Contract(pure = true)
    public Guild(long id, @NotNull String name, @NotNull String format, int capacity, boolean deleted) {
        this.id = id;
        this.name = name;
        this.format = format;
        this.capacity = capacity;
        this.deleted = deleted;
    }

    @Contract("_ -> new")
    public static @NotNull Guild createByResultSet(@NotNull ResultSet rs) {
        try {
            long id = rs.getLong("id");
            String name = rs.getString("name");
            String format = rs.getString("format");
            int capacity = rs.getInt("capacity");
            boolean deleted = rs.getBoolean("deleted");
            return new Guild(id, name, format, capacity, deleted);
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
}
