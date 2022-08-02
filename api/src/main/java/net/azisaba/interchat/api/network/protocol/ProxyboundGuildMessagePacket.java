package net.azisaba.interchat.api.network.protocol;

import io.netty.buffer.ByteBuf;
import net.azisaba.interchat.api.network.Packet;
import net.azisaba.interchat.api.network.ProxyPacketListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ProxyboundGuildMessagePacket extends Packet<ProxyPacketListener> {
    private final long guildId;
    private final String server;
    private final UUID sender;
    private final String message;

    public ProxyboundGuildMessagePacket(long guildId, @NotNull String server, @NotNull UUID sender, @NotNull String message) {
        this.guildId = guildId;
        this.server = server;
        this.sender = sender;
        this.message = message;
    }

    public ProxyboundGuildMessagePacket(@NotNull ByteBuf buf) {
        this.guildId = buf.readLong();
        this.server = readString(buf);
        this.sender = readUUID(buf);
        this.message = readString(buf);
    }

    @Override
    public void encode(@NotNull ByteBuf buf) {
        buf.writeLong(guildId);
        writeString(buf, server);
        writeUUID(buf, sender);
        writeString(buf, message);
    }

    @Override
    public void handle(@NotNull ProxyPacketListener packetListener) {
        packetListener.handleGuildMessage(this);
    }

    public long guildId() {
        return guildId;
    }

    @NotNull
    public String server() {
        return server;
    }

    @NotNull
    public UUID sender() {
        return sender;
    }

    @NotNull
    public String message() {
        return message;
    }
}
