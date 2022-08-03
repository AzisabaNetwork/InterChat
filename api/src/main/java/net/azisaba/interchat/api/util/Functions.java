package net.azisaba.interchat.api.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class Functions {
    private Functions() { throw new AssertionError(); }

    @Contract(value = "_ -> new", pure = true)
    @NotNull
    public static <T, R> Function<T, R> memoize(@NotNull Function<T, R> function) {
        return new Function<T, R>() {
            private final Map<T, R> cache = new ConcurrentHashMap<>();

            @Override
            public R apply(T t) {
                return cache.computeIfAbsent(t, function);
            }
        };
    }

    @Contract(value = "_, _ -> new", pure = true)
    @NotNull
    public static <T, R> Function<T, R> memoize(long validForMillis, @NotNull Function<T, R> function) {
        return new Function<T, R>() {
            private final Map<T, Map.Entry<Long, R>> cache = new ConcurrentHashMap<>();

            @Override
            public R apply(T t) {
                Map.Entry<Long, R> entry = cache.computeIfAbsent(t, k ->
                        new AbstractMap.SimpleImmutableEntry<>(System.currentTimeMillis(), function.apply(k)));
                if (entry.getKey() + validForMillis < System.currentTimeMillis()) {
                    entry = new AbstractMap.SimpleImmutableEntry<>(System.currentTimeMillis(), function.apply(t));
                    cache.put(t, entry);
                }
                return entry.getValue();
            }
        };
    }

    @Contract(value = "_, _ -> new", pure = true)
    @NotNull
    public static <T, U, R> BiFunction<T, U, R> memoize(long validForMillis, @NotNull BiFunction<T, U, R> function) {
        return new BiFunction<T, U, R>() {
            private final Map<T, Map<U, Map.Entry<Long, R>>> cache = new ConcurrentHashMap<>();

            @Override
            public R apply(T t, U u) {
                Map<U, Map.Entry<Long, R>> cache2 = cache.computeIfAbsent(t, k -> new ConcurrentHashMap<>());
                Map.Entry<Long, R> entry = cache2.computeIfAbsent(u, k ->
                        new AbstractMap.SimpleImmutableEntry<>(System.currentTimeMillis(), function.apply(t, k)));
                if (entry.getKey() + validForMillis < System.currentTimeMillis()) {
                    entry = new AbstractMap.SimpleImmutableEntry<>(System.currentTimeMillis(), function.apply(t, u));
                    cache2.put(u, entry);
                }
                return entry.getValue();
            }
        };
    }
}
