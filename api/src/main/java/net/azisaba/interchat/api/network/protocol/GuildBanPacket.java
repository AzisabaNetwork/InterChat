package net.azisaba.interchat.api.network.protocol;

import io.netty.buffer.ByteBuf;
import net.azisaba.interchat.api.network.Packet;
import net.azisaba.interchat.api.network.PacketListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class GuildBanPacket extends Packet<PacketListener> {
    private final long guildId;
    private final UUID actor;
    private final UUID player;
    private final String reason;

    public GuildBanPacket(long guildId, @NotNull UUID actor, @NotNull UUID player, @Nullable String reason) {
        this.guildId = guildId;
        this.actor = actor;
        this.player = player;
        this.reason = reason;
    }

    public GuildBanPacket(@NotNull ByteBuf buf) {
        this.guildId = buf.readLong();
        this.actor = readUUID(buf);
        this.player = readUUID(buf);
        this.reason = readString(buf);
    }

    @Override
    public void encode(@NotNull ByteBuf buf) {
        buf.writeLong(guildId);
        writeUUID(buf, actor);
        writeUUID(buf, player);
        writeString(buf, reason);
    }

    @Override
    public void handle(@NotNull PacketListener packetListener) {
        packetListener.handleGuildBan(this);
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

    @NotNull
    public String reason() {
        return reason;
    }
}
