package net.azisaba.interchat.velocity.command;

import com.google.common.hash.Hashing;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.azisaba.interchat.api.Logger;
import net.azisaba.interchat.api.guild.Guild;
import net.azisaba.interchat.api.guild.GuildRole;
import net.azisaba.interchat.api.network.Protocol;
import net.azisaba.interchat.api.network.protocol.GuildSoftDeletePacket;
import net.azisaba.interchat.velocity.VelocityPlugin;
import net.azisaba.interchat.velocity.command.argument.GuildArgumentType;
import net.azisaba.interchat.velocity.command.argument.GuildRoleArgumentType;
import net.azisaba.interchat.velocity.command.argument.UUIDArgumentType;
import net.azisaba.interchat.velocity.database.DatabaseManager;
import net.azisaba.interchat.velocity.text.VMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

public class GuildAdminCommand extends AbstractCommand {
    @Override
    protected @NotNull LiteralArgumentBuilder<CommandSource> createBuilder() {
        return literal("guildadmin")
                .requires(source -> source.hasPermission("interchat.guildadmin"))
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
                        )
                );
    }

    private static int executeGuildSoftDelete(@NotNull CommandSource source, @NotNull Guild guild) {
        try {
            DatabaseManager.get().runPrepareStatement("UPDATE `guilds` SET `deleted` = 1 WHERE `id` = ?", stmt -> {
                stmt.setLong(1, guild.id());
                stmt.executeUpdate();
            });
            DatabaseManager.get().submitLog(guild.id(), source, "Deleted guild (soft)");
            DatabaseManager.get().runPrepareStatement("UPDATE `players` SET `selected_guild` = -1 WHERE `selected_guild` = ?", stmt -> {
                stmt.setLong(1, guild.id());
                stmt.executeUpdate();
            });
            source.sendMessage(Component.text(VMessages.format(source, "command.guild.delete.success"), NamedTextColor.GREEN));
            if (source instanceof Player player) {
                // notify others
                GuildSoftDeletePacket packet = new GuildSoftDeletePacket(guild.id(), player.getUniqueId());
                VelocityPlugin.getPlugin().getJedisBox().getPubSubHandler().publish(Protocol.GUILD_SOFT_DELETE_PACKET.getName(), packet);
            }
        } catch (SQLException e) {
            Logger.getCurrentLogger().error("Failed to delete guild " + guild.id(), e);
            source.sendMessage(Component.text(VMessages.format(source, "command.guild.delete.error"), NamedTextColor.RED));
        }
        return 0;
    }

    @SuppressWarnings("UnstableApiUsage")
    private static int executeGuildHardDelete(@NotNull CommandSource source, @NotNull Guild guild, @Nullable Long hash) {
        long actualHash = Hashing.sha256().hashLong(guild.id()).asLong();
        if (hash == null || actualHash != hash) {
            source.sendMessage(Component.text(VMessages.format(source, "command.guildadmin.guild.hard_delete.confirm.line1"), NamedTextColor.RED));
            source.sendMessage(Component.text(VMessages.format(source, "command.guildadmin.guild.hard_delete.confirm.line2", "/guildadmin guild " + guild.name() + " hard-delete " + actualHash), NamedTextColor.RED));
        } else {
            try {
                DatabaseManager.get().runPrepareStatement("DELETE FROM `guilds` WHERE `id` = ?", stmt -> {
                    stmt.setLong(1, guild.id());
                    stmt.executeUpdate();
                });
                DatabaseManager.get().runPrepareStatement("DELETE FROM `guild_members` WHERE `guild_id` = ?", stmt -> {
                    stmt.setLong(1, guild.id());
                    stmt.executeUpdate();
                });
                DatabaseManager.get().runPrepareStatement("UPDATE `players` SET `selected_guild` = -1 WHERE `selected_guild` = ?", stmt -> {
                    stmt.setLong(1, guild.id());
                    stmt.executeUpdate();
                });
                DatabaseManager.get().submitLog(guild.id(), source, "Deleted guild (hard)");
                source.sendMessage(Component.text(VMessages.format(source, "command.guildadmin.guild.hard_delete.success"), NamedTextColor.GREEN));
            } catch (SQLException e) {
                Logger.getCurrentLogger().error("Failed to delete guild " + guild.id(), e);
                source.sendMessage(Component.text(VMessages.format(source, "command.guildadmin.guild.hard_delete.error"), NamedTextColor.RED));
            }
        }
        return 0;
    }

    private static int executeGuildRestore(@NotNull CommandSource source, @NotNull Guild guild) {
        try {
            DatabaseManager.get().runPrepareStatement("UPDATE `guilds` SET `deleted` = 0 WHERE `id` = ?", stmt -> {
                stmt.setLong(1, guild.id());
                stmt.executeUpdate();
            });
            DatabaseManager.get().submitLog(guild.id(), source, "Restored guild");
            source.sendMessage(Component.text(VMessages.format(source, "command.guildadmin.guild.restore.success"), NamedTextColor.GREEN));
        } catch (SQLException e) {
            Logger.getCurrentLogger().error("Failed to restore the guild {}", guild.id(), e);
            source.sendMessage(Component.text(VMessages.format(source, "command.guildadmin.guild.restore.error"), NamedTextColor.RED));
        }
        return 0;
    }

    private static int executeGuildRole(@NotNull CommandSource source, @NotNull Guild guild, @NotNull UUID uuid, @NotNull GuildRole role) {
        try {
            DatabaseManager.get().runPrepareStatement("INSERT INTO `guild_members` (`guild_id`, `uuid`, `role`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `role` = VALUES(`role`)", stmt -> {
                stmt.setLong(1, guild.id());
                stmt.setString(2, uuid.toString());
                stmt.setString(3, role.name());
                stmt.executeUpdate();
            });
            DatabaseManager.get().submitLog(guild.id(), source, "Set role of " + uuid + " to " + role.name());
            source.sendMessage(Component.text(VMessages.format(source, "command.guildadmin.guild.role.success", uuid.toString(), role.name()), NamedTextColor.GREEN));
        } catch (SQLException e) {
            Logger.getCurrentLogger().error("Failed to set guild ({}) role of {} to {}", guild.id(), uuid, role, e);
            source.sendMessage(Component.text(VMessages.format(source, "command.error.generic", e.getMessage()), NamedTextColor.RED));
        }
        return 0;
    }
}
