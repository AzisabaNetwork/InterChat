package net.azisaba.interchat.api.network.protocol;

import io.netty.buffer.ByteBuf;
import net.azisaba.interchat.api.network.Packet;
import net.azisaba.interchat.api.network.PacketListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class GuildSoftDeletePacket extends Packet<PacketListener> {
    private final long guildId;
    private final UUID actor;

    public GuildSoftDeletePacket(long guildId, @NotNull UUID actor) {
        this.guildId = guildId;
        this.actor = actor;
    }

    public GuildSoftDeletePacket(@NotNull ByteBuf buf) {
        this.guildId = buf.readLong();
        this.actor = readUUID(buf);
    }

    @Override
    public void encode(@NotNull ByteBuf buf) {
        buf.writeLong(guildId);
        writeUUID(buf, actor);
    }

    @Override
    public void handle(@NotNull PacketListener packetListener) {
        packetListener.handleGuildSoftDelete(this);
    }

    public long guildId() {
        return guildId;
    }

    @NotNull
    public UUID actor() {
        return actor;
    }
}
