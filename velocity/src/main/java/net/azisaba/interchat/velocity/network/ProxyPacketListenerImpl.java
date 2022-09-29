package net.azisaba.interchat.velocity.network;

import net.azisaba.interchat.api.Logger;
import net.azisaba.interchat.api.guild.Guild;
import net.azisaba.interchat.api.guild.GuildInviteResult;
import net.azisaba.interchat.api.guild.GuildManager;
import net.azisaba.interchat.api.guild.GuildMember;
import net.azisaba.interchat.api.network.ProxyPacketListener;
import net.azisaba.interchat.api.network.protocol.GuildInvitePacket;
import net.azisaba.interchat.api.network.protocol.GuildInviteResultPacket;
import net.azisaba.interchat.api.network.protocol.GuildKickPacket;
import net.azisaba.interchat.api.network.protocol.GuildLeavePacket;
import net.azisaba.interchat.api.network.protocol.GuildMessagePacket;
import net.azisaba.interchat.api.network.protocol.GuildSoftDeletePacket;
import net.azisaba.interchat.api.text.MessageFormatter;
import net.azisaba.interchat.api.user.User;
import net.azisaba.interchat.api.user.UserManager;
import net.azisaba.interchat.api.util.AsyncUtil;
import net.azisaba.interchat.api.util.MoreObjects;
import net.azisaba.interchat.velocity.VelocityPlugin;
import net.azisaba.interchat.velocity.command.GuildCommand;
import net.azisaba.interchat.velocity.listener.ChatListener;
import net.azisaba.interchat.velocity.text.VMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ProxyPacketListenerImpl implements ProxyPacketListener {
    private static final Component separator = Component.text("------------------------------", NamedTextColor.YELLOW); // 30 -'s
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
            if (guild == null || user == null || !guild.deleted()) {
                return;
            }
            List<GuildMember> members = guildManager.getMembers(guild).join();
            String formattedText = MessageFormatter.format(
                    guild.format(),
                    guild,
                    packet.server(),
                    user,
                    packet.message(),
                    packet.transliteratedMessage());
            Component formattedComponent = VMessages.fromLegacyText(formattedText);
            Logger.getCurrentLogger().info("[Guild Chat - {}] {} : {}", guild.name(), user.name(), VMessages.toPlainText(formattedComponent));
            members.forEach(member -> plugin.getServer().getPlayer(member.uuid()).ifPresent(player -> player.sendMessage(formattedComponent)));
        });
    }

    @Override
    public void handleGuildSoftDelete(@NotNull GuildSoftDeletePacket packet) {
        ChatListener.removeCacheWithGuildId(packet.guildId());
        GuildManager guildManager = plugin.getAPI().getGuildManager();
        CompletableFuture<Guild> guildFuture = guildManager.fetchGuildById(packet.guildId());
        CompletableFuture<User> userFuture = plugin.getAPI().getUserManager().fetchUser(packet.actor());
        AsyncUtil.collectAsync(guildFuture, userFuture, (guild, user) -> {
            if (guild == null || user == null) {
                return;
            }
            List<GuildMember> members = guildManager.getMembers(guild).join();
            members.forEach(member -> plugin.getServer().getPlayer(member.uuid()).ifPresent(player -> {
                Component component = VMessages.formatComponent(player, "guild.soft_deleted", user.name(), guild.name()).color(NamedTextColor.GOLD);
                player.sendMessage(separator);
                player.sendMessage(component);
                player.sendMessage(separator);
            }));
        });
    }

    @Override
    public void handleGuildInvite(@NotNull GuildInvitePacket packet) {
        GuildManager guildManager = plugin.getAPI().getGuildManager();
        UserManager userManager = plugin.getAPI().getUserManager();
        CompletableFuture<Guild> guildFuture = guildManager.fetchGuildById(packet.guildId());
        CompletableFuture<User> fromFuture = userManager.fetchUser(packet.from());
        CompletableFuture<User> toFuture = userManager.fetchUser(packet.to());
        AsyncUtil.collectAsync(guildFuture, fromFuture, toFuture, (guild, from, to) -> {
            if (guild == null || from == null || to == null) {
                return;
            }
            plugin.getServer().getPlayer(to.id()).ifPresent(player -> {
                Component component = VMessages.formatComponent(player, "guild.invited_target.title", from.name(), guild.name()).color(NamedTextColor.GOLD);
                List<Component> line2 = new ArrayList<>();
                line2.add(Component.text("[", NamedTextColor.GREEN)
                        .append(VMessages.formatComponent(player, "generic.accept"))
                        .append(Component.text("]"))
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/" + GuildCommand.COMMAND_NAME + " accept " + guild.name())));
                line2.add(Component.text(" "));
                line2.add(Component.text("[", NamedTextColor.RED)
                        .append(VMessages.formatComponent(player, "generic.reject"))
                        .append(Component.text("]"))
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/" + GuildCommand.COMMAND_NAME + " reject " + guild.name())));
                player.sendMessage(separator);
                player.sendMessage(component);
                player.sendMessage(Component.join(JoinConfiguration.noSeparators(), line2));
                player.sendMessage(separator);
            });
            List<GuildMember> members = guildManager.getMembers(guild).join();
            members.forEach(member -> plugin.getServer().getPlayer(member.uuid()).ifPresent(player -> {
                Component component =
                        VMessages.formatComponent(player, "guild.invited_member", from.name(), to.name(), guild.name())
                                .color(NamedTextColor.GOLD);
                player.sendMessage(component);
            }));
        });
    }

    @Override
    public void handleGuildInviteResult(@NotNull GuildInviteResultPacket packet) {
        CompletableFuture<Guild> guildFuture = plugin.getAPI().getGuildManager().fetchGuildById(packet.guildId());
        CompletableFuture<User> userFuture = plugin.getAPI().getUserManager().fetchUser(packet.to());
        AsyncUtil.collectAsync(guildFuture, userFuture, (guild, user) -> {
            if (guild == null || user == null) {
                return;
            }
            String key = MoreObjects.getIf(packet.result() == GuildInviteResult.ACCEPTED, () -> "guild.joined", () -> "guild.invite_rejected");
            List<GuildMember> members = guild.getMembers().join();
            members.forEach(member -> plugin.getServer().getPlayer(member.uuid()).ifPresent(player -> {
                Component component =
                        VMessages.formatComponent(player, key, user.name(), guild.name())
                                .color(NamedTextColor.GOLD);
                player.sendMessage(component);
            }));
        });
    }

    @Override
    public void handleGuildLeave(@NotNull GuildLeavePacket packet) {
        CompletableFuture<Guild> guildFuture = plugin.getAPI().getGuildManager().fetchGuildById(packet.guildId());
        CompletableFuture<User> userFuture = plugin.getAPI().getUserManager().fetchUser(packet.player());
        AsyncUtil.collectAsync(guildFuture, userFuture, (guild, user) -> {
            if (guild == null || user == null) {
                return;
            }
            List<GuildMember> members = guild.getMembers().join();
            members.forEach(member -> plugin.getServer().getPlayer(member.uuid()).ifPresent(player -> {
                Component component =
                        VMessages.formatComponent(player, "guild.left", user.name(), guild.name())
                                .color(NamedTextColor.GOLD);
                player.sendMessage(component);
            }));
        });
    }

    @Override
    public void handleGuildKick(@NotNull GuildKickPacket packet) {
        CompletableFuture<Guild> guildFuture = plugin.getAPI().getGuildManager().fetchGuildById(packet.guildId());
        CompletableFuture<User> actorFuture = plugin.getAPI().getUserManager().fetchUser(packet.actor());
        CompletableFuture<User> userFuture = plugin.getAPI().getUserManager().fetchUser(packet.player());
        AsyncUtil.collectAsync(guildFuture, actorFuture, userFuture, (guild, actor, user) -> {
            if (guild == null || actor == null || user == null) {
                return;
            }
            List<GuildMember> members = guild.getMembers().join();
            members.forEach(member -> plugin.getServer().getPlayer(member.uuid()).ifPresent(player -> {
                Component component =
                        VMessages.formatComponent(player, "guild.kicked", actor.name(), user.name(), guild.name())
                                .color(NamedTextColor.GOLD);
                player.sendMessage(component);
            }));
        });
    }
}
