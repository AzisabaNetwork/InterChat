package net.azisaba.interchat.api.data;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class DummyUserDataProvider implements UserDataProvider {
    public static final DummyUserDataProvider INSTANCE = new DummyUserDataProvider();

    @Override
    public @NotNull Map<@NotNull String, @NotNull String> getPrefix(@NotNull UUID uuid) {
        return Collections.emptyMap();
    }

    @Override
    public @NotNull Map<@NotNull String, @NotNull String> getSuffix(@NotNull UUID uuid) {
        return Collections.emptyMap();
    }
}
