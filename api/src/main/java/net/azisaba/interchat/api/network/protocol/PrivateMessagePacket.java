package net.azisaba.interchat.api.network.protocol;

import io.netty.buffer.ByteBuf;
import net.azisaba.interchat.api.network.Packet;
import net.azisaba.interchat.api.network.PacketListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PrivateMessagePacket extends Packet<PacketListener> {
    public static final String FORMAT = "&b[\uD83D\uDCAC Private] &f%s-prefix%s-playername%s-suffix &f(&e%s-server&f) &b-> &e%r-playername&a: &f%msg &8%prereplace-b";
    private final UUID sender;
    private final UUID receiver;
    private final String server;
    private final String message;
    @Nullable
    private final String transliteratedMessage;

    public PrivateMessagePacket(@NotNull UUID sender, @NotNull UUID receiver, @NotNull String server, @NotNull String message, @Nullable String transliteratedMessage) {
        this.sender = sender;
        this.receiver = receiver;
        this.server = server;
        this.message = message;
        this.transliteratedMessage = transliteratedMessage;
    }

    public PrivateMessagePacket(@NotNull ByteBuf buf) {
        this.sender = readUUID(buf);
        this.receiver = readUUID(buf);
        this.server = readString(buf);
        this.message = readString(buf);
        if (buf.readBoolean()) {
            this.transliteratedMessage = readString(buf);
        } else {
            this.transliteratedMessage = null;
        }
    }

    @Override
    public void encode(@NotNull ByteBuf buf) {
        writeUUID(buf, sender);
        writeUUID(buf, receiver);
        writeString(buf, server);
        writeString(buf, message);
        buf.writeBoolean(transliteratedMessage != null);
        if (transliteratedMessage != null) {
            writeString(buf, transliteratedMessage);
        }
    }

    @Override
    public void handle(@NotNull PacketListener packetListener) {
        packetListener.handlePrivateMessage(this);
    }

    @NotNull
    public UUID sender() {
        return sender;
    }

    @NotNull
    public UUID receiver() {
        return receiver;
    }

    @NotNull
    public String server() {
        return server;
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
