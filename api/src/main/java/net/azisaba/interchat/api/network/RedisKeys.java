package net.azisaba.interchat.api.network;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class RedisKeys {
    private RedisKeys() {}

    //public static final String PUBSUB = "interchat:pubsub";

    @Contract(pure = true)
    public static byte @NotNull [] azisabaReportPlayerPos(@NotNull UUID uuid) {
        return ("azisaba_report:player_pos:" + uuid).getBytes(StandardCharsets.UTF_8);
    }
}
