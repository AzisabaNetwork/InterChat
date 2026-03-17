package net.azisaba.interchat.api.data;

import org.jetbrains.annotations.NotNull;
import xyz.acrylicstyle.util.serialization.codec.Codec;

import java.util.UUID;

public final class PlayerPresenceData {
    public static final Codec<PlayerPresenceData> CODEC =
            Codec.<PlayerPresenceData>builder()
                    .group(
                            Codec.UUID.fieldOf("uuid").getter(PlayerPresenceData::uuid),
                            Codec.STRING.fieldOf("server").getter(PlayerPresenceData::server),
                            Codec.LONG.fieldOf("lastSeen").getter(PlayerPresenceData::lastSeen),
                            Codec.INT.fieldOf("cause").getter(data -> data.cause.ordinal())
                    )
                    .build(PlayerPresenceData::new);

    private final @NotNull UUID uuid;
    private final @NotNull String server;
    private final long lastSeen;
    private final @NotNull Cause cause;

    public PlayerPresenceData(@NotNull UUID uuid, @NotNull String server, long lastSeen, @NotNull Cause cause) {
        this.uuid = uuid;
        this.server = server;
        this.lastSeen = lastSeen;
        this.cause = cause;
    }

    private PlayerPresenceData(@NotNull UUID uuid, @NotNull String server, long lastSeen, int cause) {
        this(uuid, server, lastSeen, Cause.values()[cause]);
    }

    public @NotNull UUID uuid() {
        return uuid;
    }

    public @NotNull String server() {
        return server;
    }

    public long lastSeen() {
        return lastSeen;
    }

    public @NotNull Cause cause() {
        return cause;
    }

    public enum Cause {
        INTERCHAT,
        OTHER,
    }
}
