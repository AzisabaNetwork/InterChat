package net.azisaba.interchat.api.network;

import net.azisaba.interchat.api.network.protocol.GuildInvitePacket;
import net.azisaba.interchat.api.network.protocol.GuildInviteResultPacket;
import net.azisaba.interchat.api.network.protocol.GuildJoinPacket;
import net.azisaba.interchat.api.network.protocol.GuildKickPacket;
import net.azisaba.interchat.api.network.protocol.GuildLeavePacket;
import net.azisaba.interchat.api.network.protocol.GuildMessagePacket;
import net.azisaba.interchat.api.network.protocol.GuildSoftDeletePacket;
import org.jetbrains.annotations.NotNull;

public interface PacketListener {
    default void handleGuildMessage(@NotNull GuildMessagePacket packet) {}
    default void handleGuildSoftDelete(@NotNull GuildSoftDeletePacket packet) {}
    default void handleGuildInvite(@NotNull GuildInvitePacket packet) {}
    default void handleGuildInviteResult(@NotNull GuildInviteResultPacket packet) {}
    default void handleGuildLeave(@NotNull GuildLeavePacket packet) {}
    default void handleGuildKick(@NotNull GuildKickPacket packet) {}
    default void handleGuildJoin(@NotNull GuildJoinPacket packet) {}
}
