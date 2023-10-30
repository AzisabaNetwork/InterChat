package net.azisaba.interchat.api.data;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserDataProvider {
    @NotNull Map<@NotNull String, @NotNull String> getPrefix(@NotNull UUID uuid);
    @NotNull Map<@NotNull String, @NotNull String> getSuffix(@NotNull UUID uuid);

    default @NotNull CompletableFuture<Void> requestUpdate(@NotNull UUID uuid, @NotNull String server) {
        if (this instanceof CompoundUserDataProvider) {
            List<CompletableFuture<?>> cfs = new ArrayList<>();
            for (UserDataProvider p : ((CompoundUserDataProvider) this).getProviders()) {
                cfs.add(p.requestUpdate(uuid, server));
            }
            return CompletableFuture.allOf(cfs.toArray(new CompletableFuture[0]));
        }
        return CompletableFuture.completedFuture(null);
    }
}
