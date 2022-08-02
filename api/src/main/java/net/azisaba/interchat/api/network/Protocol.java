package net.azisaba.interchat.api.network;

import io.netty.buffer.ByteBuf;
import net.azisaba.interchat.api.network.protocol.GuildMessagePacket;
import net.azisaba.interchat.api.network.protocol.GuildSoftDeletePacket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Packet structure is as follows:
 * <ul>
 *     <li>Packet ID (int + char sequence)</li>
 *     <li>(Additional data, if any)</li>
 * </ul>
 */
public final class Protocol {
    private static final Map<String, NamedPacket<?, ?>> PACKET_MAP = new ConcurrentHashMap<>();

    // Proxy -> Backend

    // Anywhere -> Anywhere
    public static final NamedPacket<PacketListener, GuildMessagePacket> GUILD_MESSAGE_PACKET = register("guild_message", GuildMessagePacket.class, GuildMessagePacket::new);
    public static final NamedPacket<PacketListener, GuildSoftDeletePacket> GUILD_SOFT_DELETE_PACKET = register("guild_soft_delete", GuildSoftDeletePacket.class, GuildSoftDeletePacket::new);

    @NotNull
    @Contract("_, _, _ -> new")
    private static <P extends PacketListener, T extends Packet<P>> NamedPacket<P, T> register(
            @NotNull String name,
            @NotNull Class<T> clazz,
            @NotNull Function<ByteBuf, T> packetConstructor
    ) {
        if (PACKET_MAP.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate packet name: " + name);
        }
        NamedPacket<P, T> packet = new NamedPacket<>(name, clazz, packetConstructor);
        PACKET_MAP.put(packet.getName(), packet);
        return packet;
    }

    @Nullable
    public static NamedPacket<?, ?> getByName(@NotNull String name) {
        return PACKET_MAP.get(name);
    }
}
