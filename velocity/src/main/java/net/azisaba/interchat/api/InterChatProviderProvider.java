package net.azisaba.interchat.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public final class InterChatProviderProvider {
    public static void register(@NotNull InterChat api) {
        InterChatProvider.register(api);
    }
}
