package net.azisaba.interchat.api.network.protocol;

import io.netty.buffer.ByteBuf;
import net.azisaba.interchat.api.network.Packet;
import net.azisaba.interchat.api.network.PacketListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class GuildKickPacket extends Packet<PacketListener> {
    private final long guildId;
    private final UUID actor;
    private final UUID player;

    public GuildKickPacket(long guildId, @NotNull UUID actor, @NotNull UUID player) {
        this.guildId = guildId;
        this.actor = actor;
        this.player = player;
    }

    public GuildKickPacket(@NotNull ByteBuf buf) {
        this.guildId = buf.readLong();
        this.actor = readUUID(buf);
        this.player = readUUID(buf);
    }

    @Override
    public void encode(@NotNull ByteBuf buf) {
        buf.writeLong(guildId);
        writeUUID(buf, actor);
        writeUUID(buf, player);
    }

    @Override
    public void handle(@NotNull PacketListener packetListener) {
        packetListener.handleGuildKick(this);
    }

    public long guildId() {
        return guildId;
    }

    @NotNull
    public UUID actor() {
        return actor;
    }

    @NotNull
    public UUID player() {
        return player;
    }
}
