package net.azisaba.interchat.api.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.azisaba.interchat.api.Logger;
import net.azisaba.interchat.api.util.ByteBufUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class PubSubHandler implements Closeable {
    private static final String CHANNEL_STRING = "interchat:pubsub";
    public static final byte @NotNull [] CHANNEL = CHANNEL_STRING.getBytes(StandardCharsets.UTF_8);
    private final Map<String, List<Consumer<ByteBuf>>> handlers = new ConcurrentHashMap<>();
    private final ArrayDeque<Consumer<byte[]>> pingPongQueue = new ArrayDeque<>();
    private final PubSubListener listener = new PubSubListener();
    private final Side side;
    private final Logger logger;
    private final JedisPool jedisPool;
    private final PacketListener packetListener;
    private final ScheduledExecutorService pingThread = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "InterChat PubSub Ping Thread");
        t.setDaemon(true);
        return t;
    });
    private final ExecutorService subscriberThread = Executors.newFixedThreadPool(1, r -> {
        Thread t = new Thread(r, "InterChat PubSub Subscriber Thread");
        t.setDaemon(true);
        return t;
    });

    public PubSubHandler(@NotNull Side side, @NotNull Logger logger, @NotNull JedisPool jedisPool, @NotNull PacketListener packetListener) {
        this.side = side;
        this.logger = logger;
        this.jedisPool = jedisPool;
        this.packetListener = packetListener;
        register();
    }

    private void loop() {
        try {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(listener, CHANNEL);
            } catch (JedisConnectionException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            logger.warn("Failed to get Jedis resource", e);
        } finally {
            subscriberThread.submit(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                loop(); // recursion
            });
        }
    }

    private void register() {
        jedisPool.getResource().close(); // check connection
        subscriberThread.submit(this::loop);
        pingThread.scheduleAtFixedRate(() -> {
            try {
                long latency = ping();
                if (latency < 0) {
                    logger.warn("Got disconnected from Redis server, attempting to reconnect... (code: {})", latency);
                    listener.unsubscribe();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    @Contract(pure = true)
    @NotNull
    public List<Consumer<ByteBuf>> getHandlerList(@NotNull String key) {
        return handlers.getOrDefault(key, Collections.emptyList());
    }

    private List<Consumer<ByteBuf>> getOrCreateHandlerList(@NotNull String key) {
        return handlers.computeIfAbsent(key, k -> new ArrayList<>());
    }

    public void subscribe(@NotNull String key, @NotNull Consumer<ByteBuf> handler) {
        if (Protocol.getByName(key) != null) {
            throw new IllegalArgumentException("Cannot subscribe to a defined packet");
        }
        getOrCreateHandlerList(key).add(handler);
    }

    public void unsubscribe(@NotNull String key, @NotNull Consumer<ByteBuf> handler) {
        getOrCreateHandlerList(key).remove(handler);
    }

    public void unsubscribeAll(@NotNull String key) {
        handlers.remove(key);
    }

    public void publish(@NotNull String key, @NotNull ByteBuf data) {
        ByteBuf buf = Unpooled.buffer();
        Packet.writeString(buf, key);
        buf.writeBytes(data);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(CHANNEL, ByteBufUtil.toByteArray(buf));
        }
    }

    public void publish(@NotNull String key, @NotNull Packet<?> packet) {
        ByteBuf buf = Unpooled.buffer();
        Packet.writeString(buf, key);
        packet.encode(buf);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(CHANNEL, ByteBufUtil.toByteArray(buf));
        }
    }

    private void processRawMessage(byte[] message) {
        ByteBuf buf = Unpooled.wrappedBuffer(message);
        String key = Packet.readString(buf);
        try {
            NamedPacket<?, ?> namedPacket = Protocol.getByName(key);
            if (namedPacket == null) {
                processUnknown(key, buf.slice());
            } else {
                handlePacket(namedPacket, buf.slice());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (buf.refCnt() > 0) {
                buf.release();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <P extends PacketListener> void handlePacket(@NotNull NamedPacket<P, ?> namedPacket, @NotNull ByteBuf buf) {
        if (side == Side.PROXY && namedPacket.getClazz().getSimpleName().startsWith("Backend")) {
            return;
        }
        if (side == Side.BACKEND && namedPacket.getClazz().getSimpleName().startsWith("Proxy")) {
            return;
        }
        Packet<P> packet = namedPacket.create(buf);
        packet.handle((P) packetListener);
    }

    private void processUnknown(@NotNull String key, @NotNull ByteBuf data) {
        getOrCreateHandlerList(key).forEach(handler -> handler.accept(data));
    }

    private long ping() {
        if (!listener.isSubscribed()) {
            return -2;
        }

        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        // TODO: maybe we should start too; i don't think it works without this
        //thread.start();
        long start = System.currentTimeMillis();

        pingPongQueue.add(arg -> thread.interrupt());
        try {
            listener.ping();
        } catch (JedisConnectionException e) {
            return -1;
        }

        try {
            thread.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return System.currentTimeMillis() - start;
    }

    @Override
    public void close() {
        subscriberThread.shutdownNow();
        pingThread.shutdownNow();
    }

    private class PubSubListener extends BinaryJedisPubSub {
        @Override
        public void onMessage(byte[] channel, byte[] message) {
            if (Arrays.equals(CHANNEL, channel)) {
                PubSubHandler.this.processRawMessage(message);
            }
        }

        @Override
        public void onPong(byte[] pattern) {
            Consumer<byte[]> consumer = pingPongQueue.poll();
            if (consumer != null) {
                try {
                    consumer.accept(pattern);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
