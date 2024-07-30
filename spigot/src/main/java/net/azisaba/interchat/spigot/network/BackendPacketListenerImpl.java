package net.azisaba.interchat.spigot.network;

import net.azisaba.interchat.api.network.BackendPacketListener;
import net.azisaba.interchat.api.network.protocol.GuildSoftDeletePacket;
import net.azisaba.interchat.spigot.SpigotPlugin;
import net.azisaba.interchat.spigot.listener.PlayerListener;
import org.jetbrains.annotations.NotNull;

public class BackendPacketListenerImpl implements BackendPacketListener {
    private final SpigotPlugin plugin;

    public BackendPacketListenerImpl(SpigotPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handleGuildSoftDelete(@NotNull GuildSoftDeletePacket packet) {
        PlayerListener.removeCacheWithGuildId(packet.guildId());
    }
}
