package net.azisaba.interchat.velocity;

import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.Logger;
import net.azisaba.interchat.api.data.UserDataProvider;
import net.azisaba.interchat.api.util.ByteStreams;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A user data provider that uses workers backend as a provider. You need to call {@link #requestUpdate(UUID, String)}
 * to populate the data.
 */
class WorkersUserDataProvider implements UserDataProvider {
    private final Map<String, Long> requestDataCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, String>> prefix = new ConcurrentHashMap<>();

    private @NotNull String getPrefixData(@NotNull UUID uuid, @NotNull String server) {
        try {
            URLConnection connection = new URL("https://interchat-userdata-worker.azisaba.workers.dev/userdata?uuid=" + uuid + "&server=" + server).openConnection();
            return ByteStreams.readString(connection.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Requests the data to be updated. This method has 60 seconds of cooldown.
     * @param uuid the player uuid
     * @param server the server
     * @return a future that completes when update completes
     */
    @Override
    public @NotNull CompletableFuture<Void> requestUpdate(@NotNull UUID uuid, @NotNull String server) {
        if (requestDataCooldown.getOrDefault(uuid + ":" + server, 0L) > System.currentTimeMillis()) {
            return CompletableFuture.completedFuture(null);
        }
        requestDataCooldown.put(uuid + ":" + server, System.currentTimeMillis() + 1000 * 60 * 5);
        CompletableFuture<Void> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                String prefixData = getPrefixData(uuid, server);
                if (!prefixData.isBlank()) {
                    this.prefix.put(uuid, Collections.singletonMap(server, prefixData));
                }
            } catch (Exception e) {
                Logger.getCurrentLogger().warn("Error fetching prefix data", e);
            } finally {
                future.complete(null);
            }
        });
        return future;
    }

    @Override
    public @NotNull Map<@NotNull String, @NotNull String> getPrefix(@NotNull UUID uuid) {
        return prefix.getOrDefault(uuid, Collections.emptyMap());
    }

    @Override
    public @NotNull Map<@NotNull String, @NotNull String> getSuffix(@NotNull UUID uuid) {
        return Collections.emptyMap();
    }
}
