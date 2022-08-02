package net.azisaba.interchat.api.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class MoreObjects {
    private MoreObjects() { throw new AssertionError(); }

    @Contract("null, _ -> null")
    public static <T, R> R mapIfNotNull(@Nullable T t, @NotNull Function<T, R> function) {
        if (t == null) {
            return null;
        }
        return function.apply(t);
    }

    @Contract(value = "null, _ -> param2; !null, _ -> param1", pure = true)
    public static <T> T requireNonNullElse(@Nullable T t, @NotNull T defaultValue) {
        if (t == null) {
            return defaultValue;
        }
        return t;
    }
}
