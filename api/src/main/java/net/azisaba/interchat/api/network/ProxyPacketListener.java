package net.azisaba.interchat.api.network;

import net.azisaba.interchat.api.network.protocol.ProxyboundGuildMessagePacket;
import org.jetbrains.annotations.NotNull;

public interface ProxyPacketListener extends PacketListener {
    void handleGuildMessage(@NotNull ProxyboundGuildMessagePacket packet);
}
