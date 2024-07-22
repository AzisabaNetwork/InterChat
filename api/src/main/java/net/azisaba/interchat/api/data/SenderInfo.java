package net.azisaba.interchat.api.data;

import net.azisaba.interchat.api.WorldPos;
import net.azisaba.interchat.api.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class SenderInfo {
    private final @NotNull User user;
    private final @NotNull String server;
    private final @Nullable String nickname;
    private final @Nullable WorldPos pos;

    public SenderInfo(@NotNull User user, @NotNull String server, @Nullable String nickname, @Nullable WorldPos pos) {
        this.user = Objects.requireNonNull(user, "user");
        this.server = Objects.requireNonNull(server, "server");
        this.nickname = nickname;
        this.pos = pos;
    }

    public @NotNull User getUser() {
        return user;
    }

    public @NotNull String getServer() {
        return server;
    }

    public @Nullable String getNickname() {
        return nickname;
    }

    public @Nullable WorldPos getPos() {
        return pos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SenderInfo that = (SenderInfo) o;
        return Objects.equals(user, that.user) && Objects.equals(server, that.server) && Objects.equals(nickname, that.nickname) && Objects.equals(pos, that.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, server, nickname, pos);
    }

    @Override
    public String toString() {
        return "SenderInfo{" +
                "user=" + user +
                ", server='" + server + '\'' +
                ", nickname='" + nickname + '\'' +
                ", pos=" + pos +
                '}';
    }
}
