package net.azisaba.interchat.api.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.azisaba.interchat.api.Logger;
import net.azisaba.interchat.api.util.ByteBufUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import xyz.acrylicstyle.util.serialization.codec.Codec;
import xyz.acrylicstyle.util.serialization.decoder.ByteBufValueDecoder;
import xyz.acrylicstyle.util.serialization.encoder.ByteBufValueEncoder;

import java.io.Closeable;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class JedisBox implements Closeable {
    private final JedisPool jedisPool;
    private final PubSubHandler pubSubHandler;

    public JedisBox(@NotNull Side side, @NotNull Logger logger, @NotNull PacketListener packetListener, @NotNull String hostname, int port, @Nullable String username, @Nullable String password) {
        this.jedisPool = createPool(hostname, port, username, password);
        this.pubSubHandler = new PubSubHandler(side, logger, this.jedisPool, packetListener);
    }

    public <A> @NotNull A get(byte @NotNull [] key, @NotNull Codec<A> codec) {
        return get(key, codec, 1000);
    }

    public <A> @NotNull A get(byte @NotNull [] key, @NotNull Codec<A> codec, int timeoutMillis) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try (Jedis jedis = jedisPool.getResource()) {
                    byte[] data = jedis.get(key);
                    if (data == null) {
                        throw new NoSuchElementException();
                    }
                    return codec.decode(new ByteBufValueDecoder(Unpooled.wrappedBuffer(data)));
                }
            }).get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public <A> void set(byte @NotNull [] key, @NotNull Codec<A> codec, @NotNull A value) {
        try (Jedis jedis = jedisPool.getResource()) {
            ByteBuf buf = Unpooled.buffer();
            codec.encode(value, new ByteBufValueEncoder(buf));
            jedis.set(key, ByteBufUtil.toByteArray(buf));
        }
    }

    @Contract(pure = true)
    @NotNull
    public JedisPool getJedisPool() {
        return jedisPool;
    }

    @Contract(pure = true)
    @NotNull
    public PubSubHandler getPubSubHandler() {
        return pubSubHandler;
    }

    @Override
    public void close() {
        getPubSubHandler().close();
        getJedisPool().close();
    }

    @Contract("_, _, _, _ -> new")
    public static @NotNull JedisPool createPool(@NotNull String hostname, int port, @Nullable String username, @Nullable String password) {
        Objects.requireNonNull(hostname, "hostname");
        if (username != null && password != null) {
            return new JedisPool(hostname, port, username, password);
        } else if (password != null) {
            return new JedisPool(new JedisPoolConfig(), hostname, port, 3000, password);
        } else if (username != null) {
            throw new IllegalArgumentException("password must not be null when username is provided");
        } else {
            return new JedisPool(new JedisPoolConfig(), hostname, port);
        }
    }
}
