package net.azisaba.interchat.api.network.protocol;

import io.netty.buffer.ByteBuf;
import net.azisaba.interchat.api.network.Packet;
import net.azisaba.interchat.api.network.PacketListener;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class GuildJoinPacket extends Packet<PacketListener> {
    private final long guildId;
    private final UUID player;

    public GuildJoinPacket(long guildId, @NotNull UUID player) {
        this.guildId = guildId;
        this.player = player;
    }

    public GuildJoinPacket(@NotNull ByteBuf buf) {
        this.guildId = buf.readLong();
        this.player = readUUID(buf);
    }

    @Override
    public void encode(@NotNull ByteBuf buf) {
        buf.writeLong(guildId);
        writeUUID(buf, player);
    }

    @Override
    public void handle(@NotNull PacketListener packetListener) {
        packetListener.handleGuildJoin(this);
    }

    public long guildId() {
        return guildId;
    }

    @Contract(pure = true)
    public @NotNull UUID player() {
        return player;
    }
}
