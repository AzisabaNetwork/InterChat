package net.azisaba.interchat.api.network;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

public final class NamedPacket<P extends PacketListener, T extends Packet<P>> {
    private final String name;
    private final Class<T> clazz;
    private final Function<@NotNull ByteBuf, @NotNull T> packetConstructor;

    public NamedPacket(@NotNull String name, @NotNull Class<T> clazz, @NotNull Function<ByteBuf, T> packetConstructor) {
        this.name = name;
        this.clazz = clazz;
        this.packetConstructor = packetConstructor;
    }

    @Contract(pure = true)
    @NotNull
    public String getName() {
        return name;
    }

    @Contract(pure = true)
    @NotNull
    public Class<T> getClazz() {
        return clazz;
    }

    @NotNull
    public T create(@NotNull ByteBuf buf) {
        return Objects.requireNonNull(packetConstructor.apply(buf), "packetConstructor returned null");
    }

    public void send(@NotNull PubSubHandler pubSubHandler, @NotNull T packet) {
        pubSubHandler.publish(name, packet);
    }
}
