package net.azisaba.interchat.api.network.protocol;

import io.netty.buffer.ByteBuf;
import net.azisaba.interchat.api.network.Packet;
import net.azisaba.interchat.api.network.PacketListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class GuildMessagePacket extends Packet<PacketListener> {
    private final long guildId;
    private final String server;
    private final UUID sender;
    private final String message;
    @Nullable
    private final String transliteratedMessage;

    public GuildMessagePacket(long guildId, @NotNull String server, @NotNull UUID sender, @NotNull String message, @Nullable String transliteratedMessage) {
        this.guildId = guildId;
        this.server = server;
        this.sender = sender;
        this.message = message;
        this.transliteratedMessage = transliteratedMessage;
    }

    public GuildMessagePacket(@NotNull ByteBuf buf) {
        this.guildId = buf.readLong();
        this.server = readString(buf);
        this.sender = readUUID(buf);
        this.message = readString(buf);
        if (buf.readBoolean()) {
            this.transliteratedMessage = readString(buf);
        } else {
            this.transliteratedMessage = null;
        }
    }

    @Override
    public void encode(@NotNull ByteBuf buf) {
        buf.writeLong(guildId);
        writeString(buf, server);
        writeUUID(buf, sender);
        writeString(buf, message);
        buf.writeBoolean(transliteratedMessage != null);
        if (transliteratedMessage != null) {
            writeString(buf, transliteratedMessage);
        }
    }

    @Override
    public void handle(@NotNull PacketListener packetListener) {
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

    @Nullable
    public String transliteratedMessage() {
        return transliteratedMessage;
    }
}
