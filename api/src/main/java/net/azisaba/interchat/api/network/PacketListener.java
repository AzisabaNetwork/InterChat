package net.azisaba.interchat.api.network;

import net.azisaba.interchat.api.network.protocol.GuildMessagePacket;
import net.azisaba.interchat.api.network.protocol.GuildSoftDeletePacket;
import org.jetbrains.annotations.NotNull;

public interface PacketListener {
    void handleGuildMessage(@NotNull GuildMessagePacket packet);

    void handleGuildSoftDelete(@NotNull GuildSoftDeletePacket packet);
}
