package net.azisaba.interchat.velocity.command;

import com.google.common.hash.Hashing;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.Logger;
import net.azisaba.interchat.api.data.LuckPermsUserDataProvider;
import net.azisaba.interchat.api.data.UserDataProvider;
import net.azisaba.interchat.api.guild.Guild;
import net.azisaba.interchat.api.guild.GuildRole;
import net.azisaba.interchat.api.network.Protocol;
import net.azisaba.interchat.api.network.protocol.GuildSoftDeletePacket;
import net.azisaba.interchat.velocity.VelocityPlugin;
import net.azisaba.interchat.velocity.command.argument.GuildArgumentType;
import net.azisaba.interchat.velocity.command.argument.GuildRoleArgumentType;
import net.azisaba.interchat.velocity.command.argument.UUIDArgumentType;
import net.azisaba.interchat.velocity.database.DatabaseManager;
import net.azisaba.interchat.velocity.listener.ChatListener;
import net.azisaba.interchat.velocity.text.VMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletionException;

public class GuildAdminCommand extends AbstractCommand {
    @Override
    protected @NotNull LiteralArgumentBuilder<CommandSource> createBuilder() {
        return literal("guildadmin")
                .requires(source -> source.hasPermission("interchat.guildadmin"))
                .then(literal("user-data")
                        .requires(source -> source instanceof Player && source.hasPermission("interchat.guildadmin.user-data"))
                        .executes(ctx -> executeUserData(ctx.getSource()))
                )
                .then(literal("clear-cache")
                        .requires(source -> source.hasPermission("interchat.guildadmin.clear-cache"))
                        .executes(ctx -> executeClearCache(ctx.getSource()))
                )
                .then(literal("guild")
                        .requires(source -> source.hasPermission("interchat.guildadmin.guild"))
                        .then(argument("guild", GuildArgumentType.guild())
                                .then(literal("soft-delete")
                                        .requires(source -> source.hasPermission("interchat.guildadmin.guild.soft-delete"))
                                        .executes(ctx -> executeGuildSoftDelete(ctx.getSource(), GuildArgumentType.get(ctx, "guild", false)))
                                )
                                .then(literal("hard-delete")
                                        .requires(source -> source.hasPermission("interchat.guildadmin.guild.hard-delete"))
                                        .executes(ctx -> executeGuildHardDelete(ctx.getSource(), GuildArgumentType.get(ctx, "guild", true), null))
                                        .then(argument("hash", LongArgumentType.longArg())
                                                .executes(ctx -> executeGuildHardDelete(ctx.getSource(), GuildArgumentType.get(ctx, "guild", true), LongArgumentType.getLong(ctx, "hash")))
                                        )
                                )
                                .then(literal("restore")
                                        .requires(source -> source.hasPermission("interchat.guildadmin.guild.restore"))
                                        .executes(ctx -> executeGuildRestore(ctx.getSource(), GuildArgumentType.get(ctx, "guild", true)))
                                )
                                .then(literal("role")
                                        .requires(source -> source.hasPermission("interchat.guildadmin.guild.role"))
                                        .then(argument("uuid", UUIDArgumentType.uuid())
                                                .suggests(suggestPlayers())
                                                .then(argument("role", GuildRoleArgumentType.guildRole())
                                                        .suggests((ctx, builder) -> suggest(Arrays.stream(GuildRole.values()).map(Enum::name).map(String::toLowerCase), builder))
                                                        .executes(ctx -> executeGuildRole(ctx.getSource(), GuildArgumentType.get(ctx, "guild", false), UUIDArgumentType.get(ctx, "uuid"), GuildRoleArgumentType.get(ctx, "role")))
                                                )
                                        )
                                )
                                .then(literal("rename")
                                        .requires(source -> source.hasPermission("interchat.guildadmin.guild.rename"))
                                        .then(argument("name", StringArgumentType.string())
                                                .executes(ctx -> executeGuildRename(ctx.getSource(), GuildArgumentType.get(ctx, "guild", false), StringArgumentType.getString(ctx, "name")))
                                        )
                                )
                                .then(literal("info")
                                        .executes(ctx -> GuildCommand.executeInfo((Player) ctx.getSource(), GuildArgumentType.get(ctx, "guild", true)))
                                )
                        )
                );
    }

    private static int executeUserData(@NotNull CommandSource source) {
        if (!(source instanceof Player player)) {
            return 0;
        }
        if (LuckPermsUserDataProvider.isAvailable()) {
            player.sendMessage(Component.text("Prefix data:"));
            LuckPermsUserDataProvider.getChatMetaNodeDataList(player.getUniqueId(), PrefixNode.class)
                    .forEach(obj -> player.sendMessage(Component.text("- " + obj)));
            player.sendMessage(Component.text("Suffix data:"));
            LuckPermsUserDataProvider.getChatMetaNodeDataList(player.getUniqueId(), SuffixNode.class)
                    .forEach(obj -> player.sendMessage(Component.text("- " + obj)));
        }
        UserDataProvider userDataProvider = InterChatProvider.get().getUserDataProvider();
        player.sendMessage(Component.text("Prefixes:"));
        userDataProvider.getPrefix(player.getUniqueId()).forEach((server, prefix) ->
                player.sendMessage(Component.text("  " + server + ": " + prefix)));
        player.sendMessage(Component.text("Suffixes:"));
        userDataProvider.getSuffix(player.getUniqueId()).forEach((server, suffix) ->
                player.sendMessage(Component.text("  " + server + ": " + suffix)));
        return 1;
    }

    private static int executeClearCache(@NotNull CommandSource source) {
        ChatListener.clearCache();
        source.sendMessage(Component.text("Cleared cache!", NamedTextColor.GREEN));
        return 0;
    }

    private static int executeGuildSoftDelete(@NotNull CommandSource source, @NotNull Guild guild) {
        try {
            DatabaseManager.get().query("UPDATE `guilds` SET `deleted` = 1 WHERE `id` = ?", stmt -> {
                stmt.setLong(1, guild.id());
                stmt.executeUpdate();
            });
            DatabaseManager.get().submitLog(guild.id(), source, "Deleted guild (soft)");
            DatabaseManager.get().query("UPDATE `players` SET `selected_guild` = -1 WHERE `selected_guild` = ?", stmt -> {
                stmt.setLong(1, guild.id());
                stmt.executeUpdate();
            });
            source.sendMessage(VMessages.formatComponent(source, "command.guild.delete.success").color(NamedTextColor.GREEN));
            if (source instanceof Player player) {
                // notify others
                GuildSoftDeletePacket packet = new GuildSoftDeletePacket(guild.id(), player.getUniqueId());
                VelocityPlugin.getPlugin().getJedisBox().getPubSubHandler().publish(Protocol.GUILD_SOFT_DELETE.getName(), packet);
            }
        } catch (SQLException e) {
            Logger.getCurrentLogger().error("Failed to delete guild " + guild.id(), e);
            source.sendMessage(VMessages.formatComponent(source, "command.guild.delete.error").color(NamedTextColor.RED));
        }
        return 0;
    }

    @SuppressWarnings("UnstableApiUsage")
    private static int executeGuildHardDelete(@NotNull CommandSource source, @NotNull Guild guild, @Nullable Long hash) {
        long actualHash = Hashing.sha256().hashLong(guild.id()).asLong();
        if (hash == null || actualHash != hash) {
            source.sendMessage(VMessages.formatComponent(source, "command.guildadmin.guild.hard_delete.confirm.line1", guild.name()).color(NamedTextColor.RED));
            source.sendMessage(VMessages.formatComponent(source, "command.guildadmin.guild.hard_delete.confirm.line2", "/guildadmin guild " + guild.name() + " hard-delete " + actualHash).color(NamedTextColor.RED));
        } else {
            try {
                DatabaseManager.get().query("DELETE FROM `guild_bans` WHERE `guild_id` = ?", stmt -> {
                    stmt.setLong(1, guild.id());
                    stmt.executeUpdate();
                });
                DatabaseManager.get().query("DELETE FROM `guild_members` WHERE `guild_id` = ?", stmt -> {
                    stmt.setLong(1, guild.id());
                    stmt.executeUpdate();
                });
                DatabaseManager.get().query("DELETE FROM `guilds` WHERE `id` = ?", stmt -> {
                    stmt.setLong(1, guild.id());
                    stmt.executeUpdate();
                });
                DatabaseManager.get().query("UPDATE `players` SET `selected_guild` = -1 WHERE `selected_guild` = ?", stmt -> {
                    stmt.setLong(1, guild.id());
                    stmt.executeUpdate();
                });
                DatabaseManager.get().query("UPDATE `players` SET `focused_guild` = -1 WHERE `focused_guild` = ?", stmt -> {
                    stmt.setLong(1, guild.id());
                    stmt.executeUpdate();
                });
                DatabaseManager.get().submitLog(guild.id(), source, "Deleted guild (hard)");
                source.sendMessage(VMessages.formatComponent(source, "command.guildadmin.guild.hard_delete.success").color(NamedTextColor.GREEN));
            } catch (SQLException e) {
                Logger.getCurrentLogger().error("Failed to delete guild " + guild.id(), e);
                source.sendMessage(VMessages.formatComponent(source, "command.guildadmin.guild.hard_delete.error").color(NamedTextColor.RED));
            }
        }
        return 0;
    }

    private static int executeGuildRestore(@NotNull CommandSource source, @NotNull Guild guild) {
        try {
            DatabaseManager.get().query("UPDATE `guilds` SET `deleted` = 0 WHERE `id` = ?", stmt -> {
                stmt.setLong(1, guild.id());
                stmt.executeUpdate();
            });
            DatabaseManager.get().submitLog(guild.id(), source, "Restored guild");
            source.sendMessage(VMessages.formatComponent(source, "command.guildadmin.guild.restore.success").color(NamedTextColor.GREEN));
        } catch (SQLException e) {
            Logger.getCurrentLogger().error("Failed to restore the guild {}", guild.id(), e);
            source.sendMessage(VMessages.formatComponent(source, "command.guildadmin.guild.restore.error").color(NamedTextColor.RED));
        }
        return 0;
    }

    private static int executeGuildRole(@NotNull CommandSource source, @NotNull Guild guild, @NotNull UUID uuid, @NotNull GuildRole role) {
        try {
            DatabaseManager.get().query("INSERT INTO `guild_members` (`guild_id`, `uuid`, `role`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `role` = VALUES(`role`)", stmt -> {
                stmt.setLong(1, guild.id());
                stmt.setString(2, uuid.toString());
                stmt.setString(3, role.name());
                stmt.executeUpdate();
            });
            DatabaseManager.get().submitLog(guild.id(), source, "Set role of " + uuid + " to " + role.name());
            source.sendMessage(VMessages.formatComponent(source, "command.guildadmin.guild.role.success", uuid.toString(), role.name()).color(NamedTextColor.GREEN));
        } catch (SQLException e) {
            Logger.getCurrentLogger().error("Failed to set guild ({}) role of {} to {}", guild.id(), uuid, role, e);
            source.sendMessage(VMessages.formatComponent(source, "command.error.generic", e.getMessage()).color(NamedTextColor.RED));
        }
        return 0;
    }

    private static int executeGuildRename(@NotNull CommandSource source, @NotNull Guild guild, @NotNull String newName) {
        try {
            InterChatProvider.get().getGuildManager().fetchGuildByName(newName).join();
            source.sendMessage(VMessages.formatComponent(source, "command.guildadmin.guild.rename.duplicate", newName).color(NamedTextColor.RED));
            return 0;
        } catch (CompletionException ignored) {}
        try {
            DatabaseManager.get().query("UPDATE `guilds` SET `name` = ? WHERE `id` = ?", stmt -> {
                stmt.setString(1, newName);
                stmt.setLong(2, guild.id());
                stmt.executeUpdate();
            });
            DatabaseManager.get().submitLog(guild.id(), source, "Renamed guild to " + newName);
            source.sendMessage(VMessages.formatComponent(source, "command.guildadmin.guild.rename.success", newName).color(NamedTextColor.GREEN));
        } catch (SQLException e) {
            Logger.getCurrentLogger().error("Failed to rename guild {} to {}", guild.id(), newName, e);
            source.sendMessage(VMessages.formatComponent(source, "command.error.generic", e.getMessage()).color(NamedTextColor.RED));
        }
        return 0;
    }
}
