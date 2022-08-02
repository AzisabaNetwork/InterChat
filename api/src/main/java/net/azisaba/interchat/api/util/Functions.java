package net.azisaba.interchat.api.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class Functions {
    private Functions() { throw new AssertionError(); }

    @Contract(value = "_ -> new", pure = true)
    @NotNull
    public static <T, R> Function<T, R> memorize(@NotNull Function<T, R> function) {
        return new Function<T, R>() {
            private final Map<T, R> cache = new ConcurrentHashMap<>();

            @Override
            public R apply(T t) {
                return cache.computeIfAbsent(t, function);
            }
        };
    }
}
