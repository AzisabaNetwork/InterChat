package net.azisaba.interchat.api.network;

import net.azisaba.interchat.api.network.protocol.*;
import org.jetbrains.annotations.NotNull;

public interface PacketListener {
    default void handlePrivateMessage(@NotNull PrivateMessagePacket packet) {}

    default void handleGuildMessage(@NotNull GuildMessagePacket packet) {}
    default void handleGuildSoftDelete(@NotNull GuildSoftDeletePacket packet) {}
    default void handleGuildInvite(@NotNull GuildInvitePacket packet) {}
    default void handleGuildInviteResult(@NotNull GuildInviteResultPacket packet) {}
    default void handleGuildLeave(@NotNull GuildLeavePacket packet) {}
    default void handleGuildKick(@NotNull GuildKickPacket packet) {}
    default void handleGuildBan(@NotNull GuildBanPacket packet) {}
    default void handleGuildJoin(@NotNull GuildJoinPacket packet) {}
}
