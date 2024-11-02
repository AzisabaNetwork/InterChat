package net.azisaba.interchat.api.data;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserDataProvider {
    @NotNull Map<@NotNull String, @NotNull String> getPrefix(@NotNull UUID uuid);
    @NotNull Map<@NotNull String, @NotNull String> getSuffix(@NotNull UUID uuid);

    default @NotNull CompletableFuture<Void> requestUpdate(@NotNull UUID uuid, @NotNull String server) {
        return CompletableFuture.completedFuture(null);
    }
}
