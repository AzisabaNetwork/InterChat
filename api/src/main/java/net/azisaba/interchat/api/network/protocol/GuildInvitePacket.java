package net.azisaba.interchat.api.network.protocol;

import io.netty.buffer.ByteBuf;
import net.azisaba.interchat.api.network.Packet;
import net.azisaba.interchat.api.network.PacketListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class GuildInvitePacket extends Packet<PacketListener> {
    private final long guildId;
    private final UUID from;
    private final UUID to;

    public GuildInvitePacket(long guildId, @NotNull UUID from, @NotNull UUID to) {
        this.guildId = guildId;
        this.from = from;
        this.to = to;
    }

    public GuildInvitePacket(@NotNull ByteBuf buf) {
        this.guildId = buf.readLong();
        this.from = readUUID(buf);
        this.to = readUUID(buf);
    }

    @Override
    public void encode(@NotNull ByteBuf buf) {
        buf.writeLong(guildId);
        writeUUID(buf, from);
        writeUUID(buf, to);
    }

    @Override
    public void handle(@NotNull PacketListener packetListener) {
        packetListener.handleGuildInvite(this);
    }

    public long guildId() {
        return guildId;
    }

    @NotNull
    public UUID from() {
        return from;
    }

    @NotNull
    public UUID to() {
        return to;
    }
}
