package net.azisaba.interchat.velocity.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.Logger;
import net.azisaba.interchat.api.guild.Guild;
import net.azisaba.interchat.api.guild.GuildMember;
import net.azisaba.interchat.api.guild.GuildRole;
import net.azisaba.interchat.api.network.Protocol;
import net.azisaba.interchat.api.network.protocol.GuildMessagePacket;
import net.azisaba.interchat.api.network.protocol.GuildSoftDeletePacket;
import net.azisaba.interchat.api.text.MessageFormatter;
import net.azisaba.interchat.api.user.User;
import net.azisaba.interchat.api.util.Functions;
import net.azisaba.interchat.velocity.VelocityPlugin;
import net.azisaba.interchat.velocity.command.argument.GuildArgumentType;
import net.azisaba.interchat.velocity.database.DatabaseManager;
import net.azisaba.interchat.velocity.text.VMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.regex.Pattern;

public class GuildCommand extends AbstractCommand {
    private static final List<String> BLOCKED_GUILD_NAMES =
            Arrays.asList("create", "format", "chat", "delete", "select", "role", "invite", "kick", "leave", "dontinviteme");
    private static final String DEFAULT_FORMAT = "&b[&a%gname&7@&6%server&b] &r%username&a: &r%msg";
    private static final Pattern GUILD_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-.+]{2,32}$");
    private static final String COMMAND_NAME = "guild_test";
    private static final Guild SAMPLE_GUILD = new Guild(0, "test", "", 100, false);
    private static final Function<String, User> SAMPLE_USERS = Functions.memoize(s ->
            new User(new UUID(0, 0), s, -1, false)
    );

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSource> createBuilder() {
        return literal(COMMAND_NAME)
                .requires(source -> source instanceof Player && source.hasPermission("interchat.guild"))
                // everyone
                .then(literal("create")
                        .requires(source -> source.hasPermission("interchat.guild.create"))
                        .then(argument("name", StringArgumentType.word())
                                .executes(ctx -> executeCreate((Player) ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                )
                // moderator+
                .then(literal("format")
                        .requires(source -> source.hasPermission("interchat.guild.format")/* && hasRoleInSelectedGuild(source, GuildRole.MODERATOR)*/)
                        .then(argument("format", StringArgumentType.greedyString())
                                .suggests((context, builder) -> {
                                    User sampleUser = SAMPLE_USERS.apply(((Player) context.getSource()).getUsername());
                                    String format = context.getLastChild()
                                            .getInput()
                                            .replaceFirst(COMMAND_NAME + " format ", "");
                                    String formatted = "\u00a7r" + MessageFormatter.format(format, SAMPLE_GUILD, "test-server", sampleUser, "test");
                                    return builder.suggest(formatted.replace('&', '\u00a7')).buildFuture();
                                })
                                .executes(ctx -> executeFormat((Player) ctx.getSource(), StringArgumentType.getString(ctx, "format")))
                        )
                )
                // member
                .then(literal("chat")
                        .requires(source -> source.hasPermission("interchat.guild.chat")/* && hasSelectedGuild(source)*/)
                        .then(argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> executeChat((Player) ctx.getSource(), StringArgumentType.getString(ctx, "message")))
                        )
                )
                // owner
                .then(literal("delete")
                        .requires(source -> source.hasPermission("interchat.guild.delete")/* && hasRoleInSelectedGuild(source, GuildRole.OWNER)*/)
                        .executes(ctx -> executeDelete((Player) ctx.getSource()))
                )
                // everyone
                .then(literal("select")
                        .requires(source -> source.hasPermission("interchat.guild.select"))
                        .then(argument("guild", GuildArgumentType.guild())
                                .suggests(suggestGuildsOfMember(false))
                                .executes(ctx -> executeSelect((Player) ctx.getSource(), GuildArgumentType.get(ctx, "guild", false)))
                        )
                );
    }

    private static int executeCreate(@NotNull Player player, @NotNull String name) {
        if (!GUILD_NAME_PATTERN.matcher(name).matches() || BLOCKED_GUILD_NAMES.contains(name.toLowerCase())) {
            player.sendMessage(Component.text(VMessages.format(player, "command.guild.create.invalid_name"), NamedTextColor.RED));
            return 1;
        }
        try {
            InterChatProvider.get().getGuildManager().fetchGuildByName(name).join();
            player.sendMessage(Component.text(VMessages.format(player, "command.guild.create.already_exists"), NamedTextColor.RED));
            return 0;
        } catch (CompletionException ignore) {
        }
        try {
            DatabaseManager db = DatabaseManager.get();
            long guildId;
            // create guild
            try (
                    Connection connection = db.getConnection();
                    PreparedStatement stmt = connection.prepareStatement("INSERT INTO `guilds` (`name`, `format`) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)
            ) {
                stmt.setString(1, name);
                stmt.setString(2, DEFAULT_FORMAT);
                if (stmt.executeUpdate() == 0) {
                    player.sendMessage(Component.text(VMessages.format(player, "command.guild.create.fail"), NamedTextColor.RED));
                    return 0;
                }
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    guildId = keys.getLong(1);
                } else {
                    player.sendMessage(Component.text(VMessages.format(player, "command.guild.create.fail"), NamedTextColor.RED));
                    return 0;
                }
                keys.close();
            }
            db.submitLog(guildId, player, "Created guild");
            // mark the player as the owner of the guild
            db.runPrepareStatement("INSERT INTO `guild_members` (`guild_id`, `uuid`, `role`) VALUES (?, ?, ?)", stmt -> {
                stmt.setLong(1, guildId);
                stmt.setString(2, player.getUniqueId().toString());
                stmt.setString(3, GuildRole.OWNER.name());
                stmt.executeUpdate();
            });
            db.submitLog(guildId, player, "Set owner to " + player.getUsername() + "(" + player.getUniqueId() + ")");
            // select the guild
            db.runPrepareStatement("UPDATE `players` SET `selected_guild` = ? WHERE `id` = ?", stmt -> {
                stmt.setLong(1, guildId);
                stmt.setString(2, player.getUniqueId().toString());
                stmt.executeUpdate();
            });
            player.sendMessage(Component.text(VMessages.format(player, "command.guild.create.success"), NamedTextColor.GREEN));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    private static int executeFormat(@NotNull Player player, @NotNull String format) {
        if (!format.contains("%msg") || !format.contains("%username")) {
            player.sendMessage(Component.text(VMessages.format(player, "command.guild.format.invalid_format"), NamedTextColor.RED));
            return 0;
        }
        long selectedGuild = ensureSelected(player);
        if (selectedGuild == -1) return 0;
        GuildMember member = InterChatProvider.get().getGuildManager().getMember(selectedGuild, player.getUniqueId()).join();
        if (GuildRole.MODERATOR.ordinal() < member.role().ordinal()) {
            // member must be at least moderator to change the format
            player.sendMessage(Component.text(VMessages.format(player, "command.guild.format.not_moderator"), NamedTextColor.RED));
            return 0;
        }
        try {
            DatabaseManager.get().runPrepareStatement("UPDATE `guilds` SET `format` = ? WHERE `id` = ?", stmt -> {
                stmt.setString(1, format);
                stmt.setLong(2, selectedGuild);
                stmt.executeUpdate();
            });
            player.sendMessage(Component.text(VMessages.format(player, "command.guild.format.success"), NamedTextColor.GREEN));
        } catch (SQLException e) {
            Logger.getCurrentLogger().error("Failed to change format of guild " + selectedGuild, e);
            player.sendMessage(Component.text(VMessages.format(player, "command.guild.format.error"), NamedTextColor.RED));
        }
        DatabaseManager.get().submitLog(selectedGuild, player, "Set format to " + format);
        return 0;
    }

    private static int executeChat(@NotNull Player player, @NotNull String message) {
        long selectedGuild = ensureSelected(player);
        if (selectedGuild == -1) return 0;
        GuildMessagePacket packet = new GuildMessagePacket(
                selectedGuild,
                player.getCurrentServer().orElseThrow(IllegalStateException::new).getServerInfo().getName(),
                player.getUniqueId(),
                message);
        VelocityPlugin.getPlugin().getJedisBox().getPubSubHandler().publish(Protocol.GUILD_MESSAGE_PACKET.getName(), packet);
        return 0;
    }

    private static int executeDelete(@NotNull Player player) {
        long selectedGuild = ensureSelected(player);
        if (selectedGuild == -1) return 0;
        GuildMember member = InterChatProvider.get().getGuildManager().getMember(selectedGuild, player.getUniqueId()).join();
        if (member.role() != GuildRole.OWNER) {
            // member must be owner
            player.sendMessage(Component.text(VMessages.format(player, "command.guild.delete.not_owner"), NamedTextColor.RED));
            return 0;
        }
        try {
            DatabaseManager.get().runPrepareStatement("UPDATE `guilds` SET `deleted` = 1 WHERE `id` = ?", stmt -> {
                stmt.setLong(1, selectedGuild);
                stmt.executeUpdate();
            });
            DatabaseManager.get().submitLog(selectedGuild, player, "Deleted guild (soft)");
            DatabaseManager.get().runPrepareStatement("UPDATE `players` SET `selected_guild` = -1 WHERE `selected_guild` = ?", stmt -> {
                stmt.setLong(1, selectedGuild);
                stmt.executeUpdate();
            });
            player.sendMessage(Component.text(VMessages.format(player, "command.guild.delete.success"), NamedTextColor.GREEN));
            // notify others
            GuildSoftDeletePacket packet = new GuildSoftDeletePacket(selectedGuild, player.getUniqueId());
            VelocityPlugin.getPlugin().getJedisBox().getPubSubHandler().publish(Protocol.GUILD_SOFT_DELETE_PACKET.getName(), packet);
        } catch (SQLException e) {
            Logger.getCurrentLogger().error("Failed to delete guild " + selectedGuild, e);
            player.sendMessage(Component.text(VMessages.format(player, "command.guild.delete.error"), NamedTextColor.RED));
        }
        return 0;
    }

    private static int executeSelect(@NotNull Player player, @NotNull Guild guild) {
        try {
            DatabaseManager.get().runPrepareStatement("UPDATE `players` SET `selected_guild` = ? WHERE `id` = ?", stmt -> {
                stmt.setLong(1, guild.id());
                stmt.setString(2, player.getUniqueId().toString());
                stmt.executeUpdate();
            });
            player.sendMessage(Component.text(VMessages.format(player, "command.guild.select.success"), NamedTextColor.GREEN));
        } catch (SQLException e) {
            Logger.getCurrentLogger().error("Failed to select guild " + guild.id(), e);
            player.sendMessage(Component.text(VMessages.format(player, "command.guild.select.error"), NamedTextColor.RED));
        }
        return 0;
    }

    private static long ensureSelected(@NotNull Player player) {
        long selectedGuild = InterChatProvider.get().getUserManager().fetchUser(player.getUniqueId()).join().selectedGuild();
        if (selectedGuild == -1) {
            player.sendMessage(Component.text(VMessages.format(player, "command.guild.not_selected", COMMAND_NAME), NamedTextColor.RED));
        }
        return selectedGuild;
    }
}
