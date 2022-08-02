package net.azisaba.interchat.velocity.network;

import net.azisaba.interchat.api.guild.Guild;
import net.azisaba.interchat.api.guild.GuildManager;
import net.azisaba.interchat.api.guild.GuildMember;
import net.azisaba.interchat.api.network.ProxyPacketListener;
import net.azisaba.interchat.api.network.protocol.ProxyboundGuildMessagePacket;
import net.azisaba.interchat.api.text.MessageFormatter;
import net.azisaba.interchat.api.user.User;
import net.azisaba.interchat.api.util.AsyncUtil;
import net.azisaba.interchat.velocity.VelocityPlugin;
import net.azisaba.interchat.velocity.text.VMessages;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ProxyPacketListenerImpl implements ProxyPacketListener {
    private final VelocityPlugin plugin;

    public ProxyPacketListenerImpl(@NotNull VelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handleGuildMessage(@NotNull ProxyboundGuildMessagePacket packet) {
        GuildManager guildManager = plugin.getAPI().getGuildManager();
        CompletableFuture<Guild> guildFuture = guildManager.fetchGuildById(packet.guildId());
        CompletableFuture<User> userFuture = plugin.getAPI().getUserManager().fetchUser(packet.sender());
        AsyncUtil.collectAsync(guildFuture, userFuture, (guild, user) -> {
            if (guild == null || user == null) {
                return;
            }
            List<GuildMember> members = guildManager.getMembers(guild).join();
            String formattedText = MessageFormatter.format(
                    guild.format(),
                    guild,
                    packet.server(),
                    user,
                    packet.message());
            Component formattedComponent = VMessages.fromLegacyText(formattedText);
            members.forEach(member -> plugin.getServer().getPlayer(member.uuid()).ifPresent(player -> player.sendMessage(formattedComponent)));
        });
    }
}
