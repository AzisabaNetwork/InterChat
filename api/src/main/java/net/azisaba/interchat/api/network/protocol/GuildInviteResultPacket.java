package net.azisaba.interchat.api.network.protocol;

import io.netty.buffer.ByteBuf;
import net.azisaba.interchat.api.guild.GuildInvite;
import net.azisaba.interchat.api.guild.GuildInviteResult;
import net.azisaba.interchat.api.network.Packet;
import net.azisaba.interchat.api.network.PacketListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class GuildInviteResultPacket extends Packet<PacketListener> {
    private final long guildId;
    private final UUID from;
    private final UUID to;
    private final GuildInviteResult result;

    public GuildInviteResultPacket(long guildId, @NotNull UUID from, @NotNull UUID to, @NotNull GuildInviteResult result) {
        this.guildId = guildId;
        this.from = from;
        this.to = to;
        this.result = result;
    }

    public GuildInviteResultPacket(@NotNull GuildInvite invite, @NotNull GuildInviteResult result) {
        this(invite.guildId(), invite.actor(), invite.target(), result);
    }

    public GuildInviteResultPacket(@NotNull ByteBuf buf) {
        this.guildId = buf.readLong();
        this.from = readUUID(buf);
        this.to = readUUID(buf);
        this.result = GuildInviteResult.values()[buf.readInt()];
    }

    @Override
    public void encode(@NotNull ByteBuf buf) {
        buf.writeLong(guildId);
        writeUUID(buf, from);
        writeUUID(buf, to);
        buf.writeInt(result.ordinal());
    }

    @Override
    public void handle(@NotNull PacketListener packetListener) {
        packetListener.handleGuildInviteResult(this);
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

    @NotNull
    public GuildInviteResult result() {
        return result;
    }
}
