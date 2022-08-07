package net.azisaba.interchat.velocity.network;

import net.azisaba.interchat.api.guild.Guild;
import net.azisaba.interchat.api.guild.GuildManager;
import net.azisaba.interchat.api.guild.GuildMember;
import net.azisaba.interchat.api.network.ProxyPacketListener;
import net.azisaba.interchat.api.network.protocol.GuildMessagePacket;
import net.azisaba.interchat.api.network.protocol.GuildSoftDeletePacket;
import net.azisaba.interchat.api.text.MessageFormatter;
import net.azisaba.interchat.api.user.User;
import net.azisaba.interchat.api.util.AsyncUtil;
import net.azisaba.interchat.velocity.VelocityPlugin;
import net.azisaba.interchat.velocity.text.VMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ProxyPacketListenerImpl implements ProxyPacketListener {
    private static final Component separator = Component.text("----------------------------------------", NamedTextColor.YELLOW); // 40 -'s
    private final VelocityPlugin plugin;

    @Contract(pure = true)
    public ProxyPacketListenerImpl(@NotNull VelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handleGuildMessage(@NotNull GuildMessagePacket packet) {
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

    @Override
    public void handleGuildSoftDelete(@NotNull GuildSoftDeletePacket packet) {
        GuildManager guildManager = plugin.getAPI().getGuildManager();
        CompletableFuture<Guild> guildFuture = guildManager.fetchGuildById(packet.guildId());
        CompletableFuture<User> userFuture = plugin.getAPI().getUserManager().fetchUser(packet.actor());
        AsyncUtil.collectAsync(guildFuture, userFuture, (guild, user) -> {
            if (guild == null || user == null) {
                return;
            }
            List<GuildMember> members = guildManager.getMembers(guild).join();
            members.forEach(member -> plugin.getServer().getPlayer(member.uuid()).ifPresent(player -> {
                Component component = Component.text(VMessages.format(player, "guild.soft_deleted", user.name(), guild.name()), NamedTextColor.GOLD);
                player.sendMessage(separator);
                player.sendMessage(component);
                player.sendMessage(separator);
            }));
        });
    }
}
