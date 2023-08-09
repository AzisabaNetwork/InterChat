package net.azisaba.interchat.api.data;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public interface UserDataProvider {
    @NotNull Map<@NotNull String, @NotNull String> getPrefix(@NotNull UUID uuid);
    @NotNull Map<@NotNull String, @NotNull String> getSuffix(@NotNull UUID uuid);
}
