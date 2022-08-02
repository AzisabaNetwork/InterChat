package net.azisaba.interchat.api.util;

import net.azisaba.interchat.api.InterChatProvider;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("DuplicatedCode")
public final class AsyncUtil {
    private AsyncUtil() { throw new AssertionError(); }

    public static <K1> void collectAsync(
            @NotNull @Async.Schedule Supplier<K1> k1,
            @NotNull @Async.Schedule Consumer<@Nullable K1> action) {
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                action.accept(k1.get());
            } catch (Throwable t) {
                action.accept(null);
            }
        });
    }

    public static <K1, K2> void collectAsync(
            @NotNull @Async.Schedule Supplier<K1> k1,
            @NotNull @Async.Schedule Supplier<K2> k2,
            @NotNull @Async.Schedule BiConsumer<@Nullable K1, @Nullable K2> action) {
        CountDownLatch latch = new CountDownLatch(2);
        AtomicReference<K1> vk1 = new AtomicReference<>();
        AtomicReference<K2> vk2 = new AtomicReference<>();
        Executor executor = InterChatProvider.get().getAsyncExecutor();
        executor.execute(() -> {
            try {
                vk1.set(k1.get());
            } finally {
                latch.countDown();
            }
        });
        executor.execute(() -> {
            try {
                vk2.set(k2.get());
            } finally {
                latch.countDown();
            }
        });
        executor.execute(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            action.accept(vk1.get(), vk2.get());
        });
    }

    public static <K1, K2> void collectAsync(
            @NotNull CompletableFuture<K1> k1,
            @NotNull CompletableFuture<K2> k2,
            @NotNull BiConsumer<@Nullable K1, @Nullable K2> action) {
        collectAsync(k1::join, k2::join, action);
    }

    public static <K1, K2, K3> void collectAsync(
            @NotNull @Async.Schedule Supplier<K1> k1,
            @NotNull @Async.Schedule Supplier<K2> k2,
            @NotNull @Async.Schedule Supplier<K3> k3,
            @NotNull @Async.Schedule Consumer3<@Nullable K1, @Nullable K2, @Nullable K3> action) {
        CountDownLatch latch = new CountDownLatch(3);
        AtomicReference<K1> vk1 = new AtomicReference<>();
        AtomicReference<K2> vk2 = new AtomicReference<>();
        AtomicReference<K3> vk3 = new AtomicReference<>();
        Executor executor = InterChatProvider.get().getAsyncExecutor();
        executor.execute(() -> {
            try {
                vk1.set(k1.get());
            } finally {
                latch.countDown();
            }
        });
        executor.execute(() -> {
            try {
                vk2.set(k2.get());
            } finally {
                latch.countDown();
            }
        });
        executor.execute(() -> {
            try {
                vk3.set(k3.get());
            } finally {
                latch.countDown();
            }
        });
        executor.execute(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            action.accept(vk1.get(), vk2.get(), vk3.get());
        });
    }

    public static <K1, K2, K3> void collectAsync(
            @NotNull CompletableFuture<K1> k1,
            @NotNull CompletableFuture<K2> k2,
            @NotNull CompletableFuture<K3> k3,
            @NotNull Consumer3<@Nullable K1, @Nullable K2, @Nullable K3> action) {
        collectAsync(k1::join, k2::join, k3::join, action);
    }

    public static <K1, K2, K3, K4> void collectAsync(
            @NotNull @Async.Schedule Supplier<K1> k1,
            @NotNull @Async.Schedule Supplier<K2> k2,
            @NotNull @Async.Schedule Supplier<K3> k3,
            @NotNull @Async.Schedule Supplier<K4> k4,
            @NotNull @Async.Schedule Consumer4<@Nullable K1, @Nullable K2, @Nullable K3, @Nullable K4> action) {
        CountDownLatch latch = new CountDownLatch(4);
        AtomicReference<K1> vk1 = new AtomicReference<>();
        AtomicReference<K2> vk2 = new AtomicReference<>();
        AtomicReference<K3> vk3 = new AtomicReference<>();
        AtomicReference<K4> vk4 = new AtomicReference<>();
        Executor executor = InterChatProvider.get().getAsyncExecutor();
        executor.execute(() -> {
            try {
                vk1.set(k1.get());
            } finally {
                latch.countDown();
            }
        });
        executor.execute(() -> {
            try {
                vk2.set(k2.get());
            } finally {
                latch.countDown();
            }
        });
        executor.execute(() -> {
            try {
                vk3.set(k3.get());
            } finally {
                latch.countDown();
            }
        });
        executor.execute(() -> {
            try {
                vk4.set(k4.get());
            } finally {
                latch.countDown();
            }
        });
        executor.execute(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            action.accept(vk1.get(), vk2.get(), vk3.get(), vk4.get());
        });
    }

    public static <K1, K2, K3, K4> void collectAsync(
            @NotNull CompletableFuture<K1> k1,
            @NotNull CompletableFuture<K2> k2,
            @NotNull CompletableFuture<K3> k3,
            @NotNull CompletableFuture<K4> k4,
            @NotNull Consumer4<@Nullable K1, @Nullable K2, @Nullable K3, @Nullable K4> action) {
        collectAsync(k1::join, k2::join, k3::join, k4::join, action);
    }
}
