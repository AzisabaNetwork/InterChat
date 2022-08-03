package net.azisaba.interchat.api.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletionException;

public class SneakyThrow {
    public static <T> T get(@NotNull ThrowableSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Throwable t) {
            if (t instanceof CompletionException) {
                sneakyThrow(t.getCause());
            }
            return sneakyThrow(t);
        }
    }

    public static void run(@NotNull ThrowableRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            if (t instanceof CompletionException) {
                sneakyThrow(t.getCause());
            }
            sneakyThrow(t);
        }
    }

    @Contract(value = "_ -> fail", pure = true)
    public static <T> @NotNull T sneakyThrow(@NotNull Throwable throwable) {
        return sneakyThrow0(throwable);
    }

    @Contract(value = "_ -> fail", pure = true)
    @SuppressWarnings("unchecked")
    private static <T, X extends Throwable> T sneakyThrow0(Throwable t) throws X {
        throw (X) t;
    }
}
