package net.azisaba.interchat.api;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Provider for {@link InterChat}. Use {@link #get()} to obtain the instance of {@link InterChat}.
 */
public final class InterChatProvider {
    private static final String NOT_LOADED_MESSAGE = "InterChat is not loaded yet!\n" +
            "Possible reasons:\n" +
            "  - the InterChat plugin is not installed or threw exception while initializing\n" +
            "  - the plugin is not in the dependency of the plugin in the stacktrace\n" +
            "  - tried to access the API before the plugin is loaded (such as constructor)\n" +
            "    Call #get() in the plugin's onEnable() method (or equivalent one) to load the API correctly!";

    private static InterChat api;

    private InterChatProvider() {
        throw new AssertionError();
    }

    /**
     * Returns the instance of {@link InterChat}.
     * @return {@link InterChat}
     * @throws IllegalStateException if the API is not loaded yet
     */
    @Contract(pure = true)
    @NotNull
    public static InterChat get() throws IllegalStateException {
        InterChat api = InterChatProvider.api;
        if (api == null) {
            throw new IllegalStateException(NOT_LOADED_MESSAGE);
        }
        return api;
    }

    @Internal
    static void register(@NotNull InterChat api) {
        if (InterChatProvider.api != null) {
            throw new IllegalStateException("API singleton already initialized");
        }
        Objects.requireNonNull(api);
        InterChatProvider.api = api;
    }
}
