package net.azisaba.interchat.api.network.protocol;

import io.netty.buffer.ByteBuf;
import net.azisaba.interchat.api.network.Packet;
import net.azisaba.interchat.api.network.PacketListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class GuildLeavePacket extends Packet<PacketListener> {
    private final long guildId;
    private final UUID player;

    public GuildLeavePacket(long guildId, @NotNull UUID player) {
        this.guildId = guildId;
        this.player = player;
    }

    public GuildLeavePacket(@NotNull ByteBuf buf) {
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
        packetListener.handleGuildLeave(this);
    }

    public long guildId() {
        return guildId;
    }

    @NotNull
    public UUID player() {
        return player;
    }
}
