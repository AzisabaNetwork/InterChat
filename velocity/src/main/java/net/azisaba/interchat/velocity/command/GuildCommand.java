package net.azisaba.interchat.velocity.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.Logger;
import net.azisaba.interchat.api.guild.Guild;
import net.azisaba.interchat.api.guild.GuildInvite;
import net.azisaba.interchat.api.guild.GuildInviteResult;
import net.azisaba.interchat.api.guild.GuildLog;
import net.azisaba.interchat.api.guild.GuildMember;
import net.azisaba.interchat.api.guild.GuildRole;
import net.azisaba.interchat.api.network.Protocol;
import net.azisaba.interchat.api.network.protocol.GuildInvitePacket;
import net.azisaba.interchat.api.network.protocol.GuildInviteResultPacket;
import net.azisaba.interchat.api.network.protocol.GuildJoinPacket;
import net.azisaba.interchat.api.network.protocol.GuildKickPacket;
import net.azisaba.interchat.api.network.protocol.GuildLeavePacket;
import net.azisaba.interchat.api.network.protocol.GuildMessagePacket;
import net.azisaba.interchat.api.text.KanaTranslator;
import net.azisaba.interchat.api.text.MessageFormatter;
import net.azisaba.interchat.api.user.User;
import net.azisaba.interchat.api.util.Functions;
import net.azisaba.interchat.api.util.ResultSetUtil;
import net.azisaba.interchat.api.util.TimeUtil;
import net.azisaba.interchat.velocity.VelocityPlugin;
import net.azisaba.interchat.velocity.command.argument.GuildArgumentType;
import net.azisaba.interchat.velocity.command.argument.GuildMemberArgumentType;
import net.azisaba.interchat.velocity.command.argument.GuildRoleArgumentType;
import net.azisaba.interchat.velocity.command.argument.UUIDArgumentType;
import net.azisaba.interchat.velocity.database.DatabaseManager;
import net.azisaba.interchat.velocity.guild.VelocityGuildManager;
import net.azisaba.interchat.velocity.listener.ChatListener;
import net.azisaba.interchat.velocity.text.VMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GuildCommand extends AbstractCommand {
    private static final int LINK_EXPIRE_MINUTES = 10;
    private static final List<String> BLOCKED_GUILD_NAMES =
            Arrays.asList("create", "format", "chat", "delete", "select", "role", "invite", "kick", "leave",
                    "dontinviteme", "toggleinvites", "accept", "reject", "info", "log", "jp-on", "jp-off",
                    "linkdiscord", "unlinkdiscord", "nick", "open", "join");
    private static final String DEFAULT_FORMAT = "&b[&a%gname&7@&6%server&b] &r%username&a: &r%msg &7%prereplace-b";
    private static final Pattern GUILD_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-.+]{2,32}$");
    public static final String COMMAND_NAME = "guild_test";
    private static final Map<UUID, Long> LAST_GUILD_INVITE = new ConcurrentHashMap<>();
    private static final Guild SAMPLE_GUILD = new Guild(0, "test", "", 100, false, false);
    private static final Function<String, User> SAMPLE_USERS = Functions.memoize(s ->
            new User(new UUID(0, 0), s, -1, -1, false, false)
    );
    private static final Function<UUID, User> ACTUAL_USER = Functions.memoize(1000 * 10, uuid ->
            InterChatProvider.get().getUserManager().fetchUser(uuid).join()
    );
    private static final Function<UUID, Guild> ACTUAL_GUILD = Functions.memoize(1000 * 10, uuid ->
            InterChatProvider.get().getGuildManager().fetchGuildById(ACTUAL_USER.apply(uuid).selectedGuild()).join()
    );
    private static final Function<Map.Entry<Long, UUID>, GuildMember> GUILD_MEMBER = Functions.memoize(1000 * 10, entry ->
            InterChatProvider.get().getGuildManager().getMember(entry.getKey(), entry.getValue()).join()
    );
    private static final Set<String> PLAYER_NAME_VARIABLES = new HashSet<>(Arrays.asList("%playername", "%username", "%username-n"));
    private static final Set<String> FORMAT_VARIABLES = new HashSet<>(Arrays.asList(
            "%gname", "%server", "%playername", "%username", "%username-n", "%msg", "%prereplace-b", "%prereplace"
    ));
    private static final ConcurrentHashMap<UUID, Long> LAST_GUILD_CREATED = new ConcurrentHashMap<>();
    public static final SuggestionProvider<CommandSource> CHAT_SUGGESTION_PROVIDER = (context, builder) -> {
        // preview and suggest
        User user = ACTUAL_USER.apply(((Player) context.getSource()).getUniqueId());
        Guild guild = ACTUAL_GUILD.apply(user.id());
        GuildMember member = GUILD_MEMBER.apply(new AbstractMap.SimpleImmutableEntry<>(guild.id(), user.id()));
        String server = ((Player) context.getSource())
                .getCurrentServer()
                .orElseThrow(IllegalStateException::new)
                .getServerInfo()
                .getName();
        String format = guild.format().replace("&r", "&r&f");
        String message = builder.getRemaining();
        String transliteratedEntire = null;
        List<String> suggestions = Collections.emptyList();
        if (message.startsWith(KanaTranslator.SKIP_CHAR_STRING)) {
            message = message.substring(1);
        } else {
            boolean translateKana = user.translateKana();
            if (translateKana) {
                suggestions = KanaTranslator.translateSync(message);
                if (!suggestions.isEmpty()) {
                    transliteratedEntire = suggestions.get(0);
                }
                try {
                    suggestions = KanaTranslator.translateSync(message.substring(message.lastIndexOf(' ') + 1));
                } catch (IndexOutOfBoundsException ignored) {}
            }
        }
        String formatted = "§r" + MessageFormatter.format(format, guild, server, user, member.nickname(), message, transliteratedEntire);
        builder.suggest(formatted.replace('&', '§'));
        if (!suggestions.isEmpty()) {
            suggestions = new ArrayList<>(suggestions);
            Collections.reverse(suggestions);
            for (String suggestion : suggestions) {
                builder.suggest(suggestion);
            }
        }
        return builder.buildFuture();
    };

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
                        .requires(source -> source.hasPermission("interchat.guild.format"))
                        .then(argument("format", StringArgumentType.greedyString())
                                .suggests((context, builder) -> {
                                    // suggest variables
                                    FORMAT_VARIABLES.forEach(builder::suggest);

                                    // format preview
                                    User sampleUser = SAMPLE_USERS.apply(((Player) context.getSource()).getUsername());
                                    String format = context.getLastChild()
                                            .getInput()
                                            .replaceFirst(COMMAND_NAME + " format ", "");
                                    String formatted = "§r" + MessageFormatter.format(format, SAMPLE_GUILD, "test-server", sampleUser, "nicked user", "tesuto", "テスト");
                                    return builder.suggest(formatted.replace('&', '§')).buildFuture();
                                })
                                .executes(ctx -> executeFormat((Player) ctx.getSource(), StringArgumentType.getString(ctx, "format")))
                        )
                )
                // member
                .then(literal("chat")
                        .requires(source -> source.hasPermission("interchat.guild.chat"))
                        .executes(ctx -> executeSetFocusedGuild((Player) ctx.getSource()))
                        .then(argument("message", StringArgumentType.greedyString())
                                .suggests(CHAT_SUGGESTION_PROVIDER)
                                .executes(ctx -> executeChat((Player) ctx.getSource(), StringArgumentType.getString(ctx, "message")))
                        )
                )
                // owner
                .then(literal("delete")
                        .requires(source -> source.hasPermission("interchat.guild.delete"))
                        .executes(ctx -> executeDelete((Player) ctx.getSource()))
                )
                // everyone
                .then(literal("select")
                        .requires(source -> source.hasPermission("interchat.guild.select"))
                        .then(argument("guild", GuildArgumentType.guild())
                                .suggests(suggestGuildsOfMember(false))
                                .executes(ctx -> executeSelect((Player) ctx.getSource(), GuildArgumentType.get(ctx, "guild", false)))
                        )
                )
                // owner
                .then(literal("role")
                        .requires(source -> source.hasPermission("interchat.guild.role"))
                        .then(argument("member", GuildMemberArgumentType.guildMember())
                                .suggests(suggestMembersOfGuild(GuildRole.OWNER))
                                .then(argument("role", GuildRoleArgumentType.guildRole())
                                        .suggests((ctx, builder) -> suggest(Arrays.stream(GuildRole.values()).map(Enum::name).map(String::toLowerCase), builder))
                                        .executes(ctx ->
                                                executeRole(
                                                        (Player) ctx.getSource(),
                                                        GuildMemberArgumentType.get(ctx, "member", GuildRole.OWNER),
                                                        GuildRoleArgumentType.get(ctx, "role")
                                                )
                                        )
                                )
                        )
                )
                // owner
                .then(literal("open")
                        .requires(source -> source.hasPermission("interchat.guild.open"))
                        .executes(ctx -> executeOpen((Player) ctx.getSource(), null))
                        .then(argument("open", BoolArgumentType.bool())
                                .executes(ctx -> executeOpen((Player) ctx.getSource(), BoolArgumentType.getBool(ctx, "open")))
                        )
                )
                // moderator
                .then(literal("invite")
                        .requires(source -> source.hasPermission("interchat.guild.invite"))
                        .then(argument("player", StringArgumentType.word())
                                .suggests(suggestPlayers())
                                .executes(ctx -> executeInvite((Player) ctx.getSource(), ctx))
                        )
                )
                // everyone
                .then(literal("accept")
                        .requires(source -> source.hasPermission("interchat.guild.accept"))
                        .then(argument("guild-name", StringArgumentType.word())
                                .executes(ctx -> executeAcceptOrReject((Player) ctx.getSource(), StringArgumentType.getString(ctx, "guild-name"), GuildInviteResult.ACCEPTED))
                        )
                )
                // everyone
                .then(literal("reject")
                        .requires(source -> source.hasPermission("interchat.guild.reject"))
                        .then(argument("guild-name", StringArgumentType.word())
                                .executes(ctx -> executeAcceptOrReject((Player) ctx.getSource(), StringArgumentType.getString(ctx, "guild-name"), GuildInviteResult.REJECTED))
                        )
                )
                // everyone
                .then(literal("dontinviteme")
                        .requires(source -> source.hasPermission("interchat.guild.toggleinvites"))
                        .executes(ctx -> executeToggleAcceptingInvites((Player) ctx.getSource()))
                )
                // everyone
                .then(literal("toggleinvites")
                        .requires(source -> source.hasPermission("interchat.guild.toggleinvites"))
                        .executes(ctx -> executeToggleAcceptingInvites((Player) ctx.getSource()))
                )
                // everyone
                .then(literal("join")
                        .requires(source -> source.hasPermission("interchat.guild.join"))
                        .then(argument("guild", StringArgumentType.word())
                                .executes(ctx -> executeJoin((Player) ctx.getSource(), StringArgumentType.getString(ctx, "guild")))
                        )
                )
                // moderator
                .then(literal("kick")
                        .requires(source -> source.hasPermission("interchat.guild.kick"))
                        .then(argument("member", GuildMemberArgumentType.guildMember())
                                .suggests(suggestMembersOfGuild(GuildRole.MODERATOR))
                                .executes(ctx -> executeKick((Player) ctx.getSource(), GuildMemberArgumentType.get(ctx, "member", GuildRole.MODERATOR)))
                        )
                )
                // member
                .then(literal("leave")
                        .requires(source -> source.hasPermission("interchat.guild.leave"))
                        .executes(ctx -> executeLeave((Player) ctx.getSource()))
                )
                // member
                .then(literal("info")
                        .requires(source -> source.hasPermission("interchat.guild.info"))
                        .executes(ctx -> executeInfo((Player) ctx.getSource(), null))
                        .then(argument("guild", GuildArgumentType.guild())
                                .suggests(suggestGuildsOfMember(false))
                                .executes(ctx -> executeInfo((Player) ctx.getSource(), GuildArgumentType.get(ctx, "guild", false)))
                        )
                )
                // member
                .then(literal("log")
                        .requires(source -> source.hasPermission("interchat.guild.log"))
                        .executes(ctx -> executeLog((Player) ctx.getSource(), 0))
                        .then(argument("page", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                                .executes(ctx -> executeLog((Player) ctx.getSource(), IntegerArgumentType.getInteger(ctx, "page")))
                        )
                )
                // member
                .then(literal("nick")
                        .requires(source -> source.hasPermission("interchat.guild.nick"))
                        .executes(ctx -> executeNick((Player) ctx.getSource(), null))
                        .then(argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> executeNick((Player) ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                )
                // everyone
                .then(literal("jp-on")
                        .requires(source -> source.hasPermission("interchat.guild.jp-on"))
                        .executes(ctx -> executeToggleTranslateKana((Player) ctx.getSource(), true))
                )
                // everyone
                .then(literal("jp-off")
                        .requires(source -> source.hasPermission("interchat.guild.jp-off"))
                        .executes(ctx -> executeToggleTranslateKana((Player) ctx.getSource(), false))
                )
                .then(literal("linkdiscord")
                        .requires(source -> source.hasPermission("interchat.guild.linkdiscord"))
                        .executes(ctx -> executeLinkDiscord((Player) ctx.getSource()))
                )
                .then(literal("unlinkdiscord")
                        .requires(source -> source.hasPermission("interchat.guild.unlinkdiscord"))
                        .executes(ctx -> executeUnlinkDiscord((Player) ctx.getSource()))
                )

                // handle special cases
                .then(argument("guild", GuildArgumentType.guild())
                        .executes(ctx -> executeSetFocusedGuild((Player) ctx.getSource(), GuildArgumentType.get(ctx, "guild", false).id()))
                        .then(argument("message", StringArgumentType.greedyString())
                                .requires(source -> source.hasPermission("interchat.guild.chat"))
                                .executes(ctx ->
                                        executeChat(
                                                (Player) ctx.getSource(),
                                                StringArgumentType.getString(ctx, "message"),
                                                GuildArgumentType.get(ctx, "guild", false).id()
                                        )
                                )
                        )
                );
    }

    private static int executeCreate(@NotNull Player player, @NotNull String name) {
        // check last created guild
        if (LAST_GUILD_CREATED.getOrDefault(player.getUniqueId(), 0L) + 1000 * 60 * 60 > System.currentTimeMillis()) {
            // 1 hour cooldown
            player.sendMessage(VMessages.formatComponent(player, "command.guild.create.cooldown").color(NamedTextColor.RED));
            return 0;
        }
        // check guild name
        if (!GUILD_NAME_PATTERN.matcher(name).matches() || BLOCKED_GUILD_NAMES.contains(name.toLowerCase())) {
            player.sendMessage(VMessages.formatComponent(player, "command.guild.create.invalid_name").color(NamedTextColor.RED));
            return 0;
        }
        // check owned guild
        if (!player.hasPermission("interchat.create_more_than_one_guild") &&
                !InterChatProvider.get().getGuildManager().getOwnedGuilds(player.getUniqueId(), false).join().isEmpty()) {
            player.sendMessage(VMessages.formatComponent(player, "command.guild.create.owned_guild").color(NamedTextColor.RED));
            return 0;
        }
        try {
            InterChatProvider.get().getGuildManager().fetchGuildByName(name).join();
            player.sendMessage(VMessages.formatComponent(player, "command.guild.create.already_exists").color(NamedTextColor.RED));
            return 0;
        } catch (CompletionException ignore) {
        }
        try {
            LAST_GUILD_CREATED.put(player.getUniqueId(), System.currentTimeMillis());
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
                    player.sendMessage(VMessages.formatComponent(player, "command.guild.create.fail").color(NamedTextColor.RED));
                    return 0;
                }
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    guildId = keys.getLong(1);
                } else {
                    player.sendMessage(VMessages.formatComponent(player, "command.guild.create.fail").color(NamedTextColor.RED));
                    return 0;
                }
                keys.close();
            }
            db.submitLog(guildId, player, "Created guild");
            // mark the player as the owner of the guild
            db.query("INSERT INTO `guild_members` (`guild_id`, `uuid`, `role`) VALUES (?, ?, ?)", stmt -> {
                stmt.setLong(1, guildId);
                stmt.setString(2, player.getUniqueId().toString());
                stmt.setString(3, GuildRole.OWNER.name());
                stmt.executeUpdate();
            });
            db.submitLog(guildId, player, "Set owner to " + player.getUsername() + "(" + player.getUniqueId() + ")");
            // select the guild
            db.query("UPDATE `players` SET `selected_guild` = ? WHERE `id` = ?", stmt -> {
                stmt.setLong(1, guildId);
                stmt.setString(2, player.getUniqueId().toString());
                stmt.executeUpdate();
            });
            player.sendMessage(VMessages.formatComponent(player, "command.guild.create.success").color(NamedTextColor.GREEN));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    private static int executeFormat(@NotNull Player player, @NotNull String format) {
        if (!format.contains("%msg") || PLAYER_NAME_VARIABLES.stream().noneMatch(format::contains)) {
            player.sendMessage(VMessages.formatComponent(player, "command.guild.format.invalid_format").color(NamedTextColor.RED));
            return 0;
        }
        long selectedGuild = ensureSelected(player);
        if (selectedGuild == -1) return 0;
        GuildMember member = InterChatProvider.get().getGuildManager().getMember(selectedGuild, player.getUniqueId()).join();
        if (GuildRole.MODERATOR.ordinal() < member.role().ordinal()) {
            // member must be at least moderator to change the format
            player.sendMessage(VMessages.formatComponent(player, "command.guild.format.not_moderator").color(NamedTextColor.RED));
            return 0;
        }
        try {
            DatabaseManager.get().query("UPDATE `guilds` SET `format` = ? WHERE `id` = ?", stmt -> {
                stmt.setString(1, format);
                stmt.setLong(2, selectedGuild);
                stmt.executeUpdate();
            });
            player.sendMessage(VMessages.formatComponent(player, "command.guild.format.success").color(NamedTextColor.GREEN));
        } catch (SQLException e) {
            Logger.getCurrentLogger().error("Failed to change format of guild " + selectedGuild, e);
            player.sendMessage(VMessages.formatComponent(player, "command.guild.format.error").color(NamedTextColor.RED));
        }
        DatabaseManager.get().submitLog(selectedGuild, player, "Set format to " + format);
        return 0;
    }

    public static int executeChat(@NotNull Player player, @NotNull String message) {
        long selectedGuild = ensureSelected(player);
        if (selectedGuild == -1) return 0;
        return executeChat(player, message, selectedGuild);
    }

    private static int executeChat(@NotNull Player player, @NotNull String message, long selectedGuild) {
        try {
            InterChatProvider.get().getGuildManager().getMember(selectedGuild, player.getUniqueId()).join();
        } catch (CompletionException e) {
            player.sendMessage(VMessages.formatComponent(player, "command.error.unknown_guild", selectedGuild).color(NamedTextColor.RED));
            return 0;
        }
        String transliteratedMessage = null;
        if (message.startsWith(KanaTranslator.SKIP_CHAR_STRING)) {
            message = message.substring(1);
        } else {
            boolean translateKana = InterChatProvider.get().getUserManager().fetchUser(player.getUniqueId()).join().translateKana();
            if (translateKana) {
                List<String> suggestions = KanaTranslator.translateSync(message);
                if (!suggestions.isEmpty()) {
                    transliteratedMessage = suggestions.get(0);
                }
            }
        }

        GuildMessagePacket packet = new GuildMessagePacket(
                selectedGuild,
                player.getCurrentServer().orElseThrow(IllegalStateException::new).getServerInfo().getName(),
                player.getUniqueId(),
                message,
                transliteratedMessage);
        VelocityPlugin.getPlugin().getJedisBox().getPubSubHandler().publish(Protocol.GUILD_MESSAGE.getName(), packet);
        return 0;
    }

    private static int executeDelete(@NotNull Player player) {
        long selectedGuild = ensureSelected(player);
        if (selectedGuild == -1) return 0;
        GuildMember member = InterChatProvider.get().getGuildManager().getMember(selectedGuild, player.getUniqueId()).join();
        if (member.role() != GuildRole.OWNER) {
            // member must be owner
            player.sendMessage(VMessages.formatComponent(player, "command.guild.delete.not_owner").color(NamedTextColor.RED));
            return 0;
        }
        try {
            VelocityGuildManager.markDeleted(player, selectedGuild);
            player.sendMessage(VMessages.formatComponent(player, "command.guild.delete.success").color(NamedTextColor.GREEN));
        } catch (Exception e) {
            Logger.getCurrentLogger().error("Failed to delete guild " + selectedGuild, e);
            player.sendMessage(VMessages.formatComponent(player, "command.guild.delete.error").color(NamedTextColor.RED));
        }
        return 0;
    }

    public static int executeSelect(@NotNull Player player, @NotNull Guild guild) {
        try {
            guild.getMember(player.getUniqueId()).join();
        } catch (CompletionException e) {
            player.sendMessage(VMessages.formatComponent(player, "command.error.unknown_guild", guild.name()).color(NamedTextColor.RED));
            return 0;
        }
        try {
            DatabaseManager.get().query("UPDATE `players` SET `selected_guild` = ? WHERE `id` = ?", stmt -> {
                stmt.setLong(1, guild.id());
                stmt.setString(2, player.getUniqueId().toString());
                stmt.executeUpdate();
            });
            player.sendMessage(VMessages.formatComponent(player, "command.guild.select.success").color(NamedTextColor.GREEN));
        } catch (SQLException e) {
            Logger.getCurrentLogger().error("Failed to select guild " + guild.id(), e);
            player.sendMessage(VMessages.formatComponent(player, "command.guild.select.error").color(NamedTextColor.RED));
        }
        return 0;
    }

    private static int executeRole(@NotNull Player player, @NotNull GuildMember member, @NotNull GuildRole role) {
        long selectedGuild = ensureSelected(player);
        if (selectedGuild == -1) return 0;
        GuildMember self = InterChatProvider.get().getGuildManager().getMember(selectedGuild, player.getUniqueId()).join();
        if (self.role() != GuildRole.OWNER) {
            // member must be owner
            player.sendMessage(VMessages.formatComponent(player, "command.guild.delete.not_owner").color(NamedTextColor.RED));
            return 0;
        }
        if (member.role() == GuildRole.OWNER && role != GuildRole.OWNER) {
            long owners = self.getGuild().join().getMembers().join().stream().filter(m -> m.role() == GuildRole.OWNER).count();
            if (owners == 1) {
                // guild must have at least one owner
                player.sendMessage(VMessages.formatComponent(player, "command.guild.role.not_enough_owners").color(NamedTextColor.RED));
                return 0;
            }
        }
        // update role
        try {
            User user = member.getUser().join();
            if (member.role() != role) {
                DatabaseManager.get().query("UPDATE `guild_members` SET `role` = ? WHERE `guild_id` = ? AND `uuid` = ?", stmt -> {
                    stmt.setString(1, role.name());
                    stmt.setLong(2, selectedGuild);
                    stmt.setString(3, member.uuid().toString());
                    stmt.executeUpdate();
                });
                DatabaseManager.get().submitLog(selectedGuild, player, "Changed role of " + member.uuid() + " (" + user.name() + ") to " + role.name());
            }
            String translatedRole = VMessages.format(player, role.getKey());
            player.sendMessage(VMessages.formatComponent(player, "command.guild.role.success", user.name(), translatedRole).color(NamedTextColor.GREEN));
        } catch (SQLException e) {
            Logger.getCurrentLogger().error("Failed to change role of " + member.uuid() + " in guild " + selectedGuild, e);
            player.sendMessage(VMessages.formatComponent(player, "command.guild.role.error").color(NamedTextColor.RED));
        }
        return 0;
    }

    private static int executeOpen(@NotNull Player player, @Nullable Boolean open) {
        long selectedGuild = ensureSelected(player);
        if (selectedGuild == -1) return 0;
        Guild guild = InterChatProvider.get().getGuildManager().fetchGuildById(selectedGuild).join();
        if (open == null) {
            // toggle
            open = !guild.open();
        }
        GuildMember self = InterChatProvider.get().getGuildManager().getMember(selectedGuild, player.getUniqueId()).join();
        if (self.role() != GuildRole.OWNER) {
            // member must be owner
            player.sendMessage(VMessages.formatComponent(player, "command.guild.delete.not_owner").color(NamedTextColor.RED));
            return 0;
        }
        if (open && !player.hasPermission("interchat.guild.open.to_public")) {
            player.sendMessage(VMessages.formatComponent(player, "command.error.permission").color(NamedTextColor.RED));
            return 0;
        }
        if (!open && !player.hasPermission("interchat.guild.open.to_private")) {
            player.sendMessage(VMessages.formatComponent(player, "command.error.permission").color(NamedTextColor.RED));
            return 0;
        }
        try {
            boolean finalOpen = open;
            DatabaseManager.get().query("UPDATE `guilds` SET `open` = ? WHERE `id` = ?", stmt -> {
                stmt.setBoolean(1, finalOpen);
                stmt.setLong(2, selectedGuild);
                stmt.executeUpdate();
            });
            DatabaseManager.get().submitLog(selectedGuild, player, "Set the guild open state to " + open);
            if (open) {
                player.sendMessage(VMessages.formatComponent(player, "command.guild.open.success.true", COMMAND_NAME, guild.name()).color(NamedTextColor.GREEN));
            } else {
                player.sendMessage(VMessages.formatComponent(player, "command.guild.open.success.false").color(NamedTextColor.GREEN));
            }
        } catch (SQLException e) {
            Logger.getCurrentLogger().error("Failed to change open state of guild " + selectedGuild, e);
            player.sendMessage(VMessages.formatComponent(player, "command.guild.open.error").color(NamedTextColor.RED));
        }
        return 0;
    }

    private static int executeInvite(@NotNull Player player, @NotNull CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        // check last guild invite
        if (!player.hasPermission("interchat.bypass_guild_invite_cooldown") && LAST_GUILD_INVITE.containsKey(player.getUniqueId())) {
            long time = LAST_GUILD_INVITE.get(player.getUniqueId());
            if (System.currentTimeMillis() - time < 1000 * 30) {
                int seconds = (int) ((1000 * 30 - (System.currentTimeMillis() - time)) / 1000);
                player.sendMessage(VMessages.formatComponent(player, "command.guild.invite.be_cool", seconds).color(NamedTextColor.RED));
                return 0;
            }
        }
        LAST_GUILD_INVITE.put(player.getUniqueId(), System.currentTimeMillis());
        // real stuff happens here
        long selectedGuild = ensureSelected(player);
        if (selectedGuild == -1) return 0;
        GuildMember member = InterChatProvider.get().getGuildManager().getMember(selectedGuild, player.getUniqueId()).join();
        if (GuildRole.MODERATOR.ordinal() < member.role().ordinal()) {
            // member must be at least moderator to change the format
            player.sendMessage(VMessages.formatComponent(player, "command.guild.invite.not_moderator").color(NamedTextColor.RED));
            return 0;
        }
        UUID targetUUID = UUIDArgumentType.getPlayerWithAPI(ctx, "player");
        // check if player is already in guild
        try {
            InterChatProvider.get().getGuildManager().getMember(selectedGuild, targetUUID).join();
            player.sendMessage(VMessages.formatComponent(player, "command.guild.invite.already_member").color(NamedTextColor.RED));
            return 0;
        } catch (CompletionException ignore) {
            // not in guild
        }
        // check for existing invite
        try {
            GuildInvite invite = InterChatProvider.get().getGuildManager().getInvite(selectedGuild, targetUUID).join();
            if (invite.isExpired()) {
                invite.delete().join(); // delete expired invite and continue
            } else {
                player.sendMessage(VMessages.formatComponent(player, "command.guild.invite.already_invited").color(NamedTextColor.RED));
                return 0;
            }
        } catch (NoSuchElementException | CompletionException ignored) {
            // no invite found
        }
        // fetch user and check if user is accepting invites
        User user = InterChatProvider.get().getUserManager().fetchUser(targetUUID).join();
        if (!user.acceptingInvites()) {
            player.sendMessage(VMessages.formatComponent(player, "command.guild.invite.not_accepting").color(NamedTextColor.RED));
            return 0;
        }
        // add invite entry to database
        try {
            DatabaseManager.get().query("INSERT INTO `guild_invites` (`guild_id`, `target`, `actor`, `expires_at`) VALUES (?, ?, ?, ?)", stmt -> {
                stmt.setLong(1, selectedGuild);
                stmt.setString(2, targetUUID.toString());
                stmt.setString(3, player.getUniqueId().toString());
                stmt.setLong(4, System.currentTimeMillis() + 1000 * 60 * 5); // 5 minutes from now
                stmt.executeUpdate();
            });
        } catch (SQLException e) {
            Logger.getCurrentLogger().error("Failed to invite " + targetUUID + " to guild " + selectedGuild, e);
            player.sendMessage(VMessages.formatComponent(player, "command.guild.invite.error").color(NamedTextColor.RED));
            return 0;
        }
        // notify others
        Protocol.GUILD_INVITE.send(
                VelocityPlugin.getPlugin().getJedisBox().getPubSubHandler(),
                new GuildInvitePacket(selectedGuild, player.getUniqueId(), targetUUID)
        );
        return 0;
    }

    private static int executeAcceptOrReject(@NotNull Player player, @NotNull String guildName, @NotNull GuildInviteResult result) {
        // fetch guild
        Guild guild;
        try {
            guild = InterChatProvider.get().getGuildManager().fetchGuildByName(guildName).join();
        } catch (CompletionException e) {
            return 0; // ignore silently
        }
        // fetch invite
        GuildInvite invite;
        try {
            invite = InterChatProvider.get().getGuildManager().getInvite(guild.id(), player.getUniqueId()).join();
            invite.delete().join(); // delete the invite, we don't need it anymore
            if (invite.isExpired()) {
                throw new NoSuchElementException("Invite expired");
            }
        } catch (NoSuchElementException | CompletionException e) {
            return 0;
        }
        // accept or reject
        if (result == GuildInviteResult.ACCEPTED) {
            // accepted
            new GuildMember(guild.id(), invite.target(), GuildRole.MEMBER).update().join();
            User actor = InterChatProvider.get().getUserManager().fetchUser(invite.actor()).join();
            // set selected guild
            try {
                DatabaseManager.get().query("UPDATE `players` SET `selected_guild` = ? WHERE `id` = ?", stmt -> {
                    stmt.setLong(1, guild.id());
                    stmt.setString(2, invite.target().toString());
                    stmt.executeUpdate();
                });
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            DatabaseManager.get().submitLog(guild.id(), player, "Accepted the invite from " + invite.actor() + " (" + actor.name() + ") and joined the guild");
        } else {
            player.sendMessage(VMessages.formatComponent(player, "command.guild.accept_reject.rejected", guild.name()).color(NamedTextColor.GREEN));
        }
        Protocol.GUILD_INVITE_RESULT.send(
                VelocityPlugin.getPlugin().getJedisBox().getPubSubHandler(),
                new GuildInviteResultPacket(invite, result)
        );
        return 0;
    }

    private static int executeToggleAcceptingInvites(@NotNull Player player) {
        // fetch user
        User user = InterChatProvider.get().getUserManager().fetchUser(player.getUniqueId()).join();
        // toggle accepting invites
        try {
            DatabaseManager.get().query("UPDATE `players` SET `accepting_invites` = ? WHERE `id` = ?", stmt -> {
                stmt.setBoolean(1, !user.acceptingInvites());
                stmt.setString(2, player.getUniqueId().toString());
                stmt.executeUpdate();
            });
            if (user.acceptingInvites()) {
                // no longer accepting
                player.sendMessage(VMessages.formatComponent(player, "command.guild.toggle_invites.now_false").color(NamedTextColor.GREEN));
            } else {
                // now accepting
                player.sendMessage(VMessages.formatComponent(player, "command.guild.toggle_invites.now_true").color(NamedTextColor.GREEN));
            }
        } catch (SQLException e) {
            Logger.getCurrentLogger().error("Failed to toggle accepting invites for " + player.getUniqueId(), e);
            player.sendMessage(VMessages.formatComponent(player, "command.guild.toggle_invites.error").color(NamedTextColor.RED));
        }
        return 0;
    }

    private static int executeJoin(@NotNull Player player, @NotNull String guildName) {
        // fetch guild
        Guild guild;
        try {
            guild = InterChatProvider.get().getGuildManager().fetchGuildByName(guildName).join();
        } catch (CompletionException e) {
            player.sendMessage(VMessages.formatComponent(player, "command.error.unknown_guild", guildName).color(NamedTextColor.RED));
            return 0;
        }
        // check if guild is public
        if (!guild.open()) {
            player.sendMessage(VMessages.formatComponent(player, "command.error.unknown_guild", guildName).color(NamedTextColor.RED));
            return 0;
        }
        // join the guild
        new GuildMember(guild.id(), player.getUniqueId(), GuildRole.MEMBER).update().join();
        // set selected guild
        try {
            DatabaseManager.get().query("UPDATE `players` SET `selected_guild` = ? WHERE `id` = ?", stmt -> {
                stmt.setLong(1, guild.id());
                stmt.setString(2, player.getUniqueId().toString());
                stmt.executeUpdate();
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        DatabaseManager.get().submitLog(guild.id(), player, player.getUsername() + " (" + player.getUniqueId() + ") joined the guild using /" + COMMAND_NAME + " join");
        Protocol.GUILD_JOIN.send(
                VelocityPlugin.getPlugin().getJedisBox().getPubSubHandler(),
                new GuildJoinPacket(guild.id(), player.getUniqueId())
        );
        return 0;
    }

    private static int executeLeave(@NotNull Player player) {
        // fetch guild
        long selectedGuild = ensureSelected(player);
        if (selectedGuild == -1) return 0;
        GuildMember self = InterChatProvider.get().getGuildManager().getMember(selectedGuild, player.getUniqueId()).join();
        if (self.role() == GuildRole.OWNER) {
            long owners =
                    InterChatProvider.get()
                            .getGuildManager()
                            .fetchGuildById(selectedGuild)
                            .join()
                            .getMembers()
                            .join()
                            .stream()
                            .filter(m -> m.role() == GuildRole.OWNER)
                            .count();
            if (owners == 1) {
                // guild must have at least one owner
                player.sendMessage(VMessages.formatComponent(player, "command.guild.role.not_enough_owners").color(NamedTextColor.RED));
                return 0;
            }
        }
        if (InterChatProvider.get().getGuildManager().getMembers(selectedGuild).join().size() == 1) {
            // if there is only one member, delete the guild.
            VelocityGuildManager.markDeleted(player, selectedGuild);
        }
        // leave guild
        InterChatProvider.get().getGuildManager().removeMember(selectedGuild, player.getUniqueId()).join();
        DatabaseManager.get().submitLog(selectedGuild, player, "Left the guild");
        player.sendMessage(VMessages.formatComponent(player, "command.guild.leave.success").color(NamedTextColor.GREEN));
        // notify others
        Protocol.GUILD_LEAVE.send(
                VelocityPlugin.getPlugin().getJedisBox().getPubSubHandler(),
                new GuildLeavePacket(selectedGuild, player.getUniqueId())
        );
        return 0;
    }

    private static int executeKick(@NotNull Player player, @NotNull GuildMember member) {
        // "player" has moderator or higher permissions
        long selectedGuild = ensureSelected(player);
        if (selectedGuild == -1) return 0;
        GuildMember self = InterChatProvider.get().getGuildManager().getMember(selectedGuild, player.getUniqueId()).join();
        if (self.role() != GuildRole.OWNER) {
            // owner can kick anyone, including other owners
            // but moderator cannot kick other moderators and owners, they can only kick members
            if (self.role().ordinal() >= member.role().ordinal()) {
                player.sendMessage(VMessages.formatComponent(player, "command.error.permission"));
                return 0;
            }
        }
        if (member.role() == GuildRole.OWNER) {
            long owners =
                    InterChatProvider.get()
                            .getGuildManager()
                            .fetchGuildById(selectedGuild)
                            .join()
                            .getMembers()
                            .join()
                            .stream()
                            .filter(m -> m.role() == GuildRole.OWNER)
                            .count();
            if (owners == 1) {
                // guild must have at least one owner
                player.sendMessage(VMessages.formatComponent(player, "command.guild.role.not_enough_owners").color(NamedTextColor.RED));
                return 0;
            }
        }
        if (InterChatProvider.get().getGuildManager().getMembers(selectedGuild).join().size() == 1) {
            // if there is only one member, delete the guild.
            VelocityGuildManager.markDeleted(player, selectedGuild);
        }
        // kick member
        InterChatProvider.get().getGuildManager().removeMember(selectedGuild, member.uuid()).join();
        User user = member.getUser().join();
        DatabaseManager.get().submitLog(selectedGuild, player, "Kicked " + member.uuid() + " (" + user.name() + ")");
        // notify others
        Protocol.GUILD_KICK.send(
                VelocityPlugin.getPlugin().getJedisBox().getPubSubHandler(),
                new GuildKickPacket(selectedGuild, player.getUniqueId(), member.uuid())
        );
        return 0;
    }

    private static int executeInfo(@NotNull Player player, @Nullable Guild guild) {
        if (guild == null) {
            long selectedGuild = ensureSelected(player);
            if (selectedGuild == -1) return 0;
            guild = InterChatProvider.get().getGuildManager().fetchGuildById(selectedGuild).join();
        }
        if (!player.hasPermission("interchat.info_any_guild")) {
            try {
                guild.getMember(player.getUniqueId()).join();
            } catch (CompletionException e) {
                player.sendMessage(VMessages.formatComponent(player, "command.error.unknown_guild", guild.name()).color(NamedTextColor.RED));
                return 0;
            }
        }
        List<GuildMember> members = guild.getMembers().join();
        Map<UUID, User> users = new HashMap<>();
        InterChatProvider.get()
                .getUserManager()
                .fetchUsers(members.stream().map(GuildMember::uuid).collect(Collectors.toList()))
                .join()
                .forEach(user -> users.put(user.id(), user));
        player.sendMessage(VMessages.formatComponent(player, "command.guild.info.title", guild.name()).color(NamedTextColor.GOLD));
        player.sendMessage(VMessages.formatComponent(player, "command.guild.info.member_count", members.size(), guild.capacity()).color(NamedTextColor.GOLD));
        for (GuildRole role : GuildRole.values()) {
            String players =
                    members.stream()
                            .filter(member -> member.role() == role)
                            .map(member -> users.get(member.uuid()).name())
                            .collect(Collectors.joining(", "));
            String translatedRole = VMessages.format(player, role.getKey());
            player.sendMessage(VMessages.formatComponent(player, "command.guild.info.role_players", translatedRole, players).color(NamedTextColor.GOLD));
        }
        player.sendMessage(Component.empty());
        player.sendMessage(VMessages.formatComponent(player, "command.guild.info.open").color(NamedTextColor.GOLD)
                .append(Component.text(guild.open()).color(guild.open() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        player.sendMessage(VMessages.formatComponent(player, "command.guild.info.format").color(NamedTextColor.GOLD)
                .append(Component.text(guild.format(), NamedTextColor.WHITE)
                        .hoverEvent(HoverEvent.showText(VMessages.formatComponent(player, "generic.hover.click_to_copy")))
                        .clickEvent(ClickEvent.copyToClipboard(guild.format()))
                )
        );
        return 0;
    }

    private static int executeLog(@NotNull Player player, @Range(from = 0, to = Integer.MAX_VALUE) int page) {
        long selectedGuild = ensureSelected(player);
        if (selectedGuild == -1) return 0;
        try {
            int maxPage = DatabaseManager.get().getPrepareStatement("SELECT COUNT(`id`) FROM `guild_logs` WHERE `guild_id` = ?", stmt -> {
                stmt.setLong(1, selectedGuild);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int count = rs.getInt(1);
                    if (count % 10 == 0) {
                        return count / 10;
                    } else {
                        return count / 10 + 1;
                    }
                } else {
                    return 1;
                }
            });
            if (page == 0) {
                page = maxPage;
            }
            List<GuildLog> logs = DatabaseManager.get().getPrepareStatement("SELECT * FROM `guild_logs` WHERE `guild_id` = ? LIMIT " + ((page - 1) * 10) + ", 10", stmt -> {
                stmt.setLong(1, selectedGuild);
                ResultSet rs = stmt.executeQuery();
                return ResultSetUtil.toList(rs, GuildLog::createFromResultSet);
            });
            player.sendMessage(VMessages.formatComponent(player, "command.guild.log.title", page, maxPage).color(NamedTextColor.GREEN));
            long currentTime = System.currentTimeMillis();
            for (GuildLog log : logs) {
                String time = TimeUtil.toRelativeTimeAbs(currentTime, log.time());
                player.sendMessage(VMessages.formatComponent(player, "command.guild.log.format", log.id(), time, log.actorName()).color(NamedTextColor.GREEN)
                        .append(Component.text(log.description(), NamedTextColor.WHITE)));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    public static int executeSetFocusedGuild(@NotNull Player player) {
        long selectedGuild = ensureSelected(player);
        if (selectedGuild == -1) return 0;
        return executeSetFocusedGuild(player, selectedGuild);
    }

    public static int executeSetFocusedGuild(@NotNull Player player, long selectedGuild) {
        Guild guild = InterChatProvider.get().getGuildManager().fetchGuildById(selectedGuild).join();
        try {
            guild.getMember(player.getUniqueId()).join();
        } catch (CompletionException e) {
            player.sendMessage(VMessages.formatComponent(player, "command.error.unknown_guild", guild.name()).color(NamedTextColor.RED));
            return 0;
        }
        User user = InterChatProvider.get().getUserManager().fetchUser(player.getUniqueId()).join();
        try {
            if (user.focusedGuild() == selectedGuild || player.getProtocolVersion().ordinal() >= ProtocolVersion.valueOf("MINECRAFT_1_19_1").ordinal()) {
                // 1.19.1+ does not support this feature for these reasons:
                // - canceling the chat breaks the chain, and verification on the server side will fail, resulting in a kick
                // - the message needs to be unsigned if we modify the chat message, but then the chain breaks
                // also see https://github.com/PaperMC/Velocity/issues/804 for more details on why this doesn't work

                // remove focused guild
                DatabaseManager.get().query("UPDATE `players` SET `focused_guild` = -1 WHERE `id` = ?", stmt -> {
                    stmt.setString(1, player.getUniqueId().toString());
                    stmt.executeUpdate();
                });
                player.sendMessage(VMessages.formatComponent(player, "command.guild.focus.unfocused", guild.name()).color(NamedTextColor.GREEN));
            } else {
                // set focused guild
                DatabaseManager.get().query("UPDATE `players` SET `focused_guild` = ? WHERE `id` = ?", stmt -> {
                    stmt.setLong(1, selectedGuild);
                    stmt.setString(2, player.getUniqueId().toString());
                    stmt.executeUpdate();
                });
                player.sendMessage(VMessages.formatComponent(player, "command.guild.focus.focused", guild.name()).color(NamedTextColor.GREEN));
            }
            ChatListener.removeCache(player.getUniqueId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    private static int executeToggleTranslateKana(@NotNull Player player, boolean flag) {
        try {
            DatabaseManager.get().query("UPDATE `players` SET `translate_kana` = ? WHERE `id` = ?", stmt -> {
                stmt.setBoolean(1, flag);
                stmt.setString(2, player.getUniqueId().toString());
                stmt.executeUpdate();
            });
            if (flag) {
                player.sendMessage(VMessages.formatComponent(player, "command.guild.translate_kana.enabled").color(NamedTextColor.GREEN));
            } else {
                player.sendMessage(VMessages.formatComponent(player, "command.guild.translate_kana.disabled").color(NamedTextColor.GREEN));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    private static int executeLinkDiscord(@NotNull Player player) {
        try {
            String guildChatDiscordName = VelocityPlugin.getPlugin().getDatabaseConfig().guildChatDiscordName();
            String linkCode = UUID.randomUUID().toString().substring(0, 8);
            DatabaseManager.get().query("SELECT `id` FROM `" + guildChatDiscordName + "`.`users` WHERE `link_code` = ?", ps -> {
                ps.setString(1, linkCode);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        throw new IllegalStateException("Link code already exists");
                    }
                }
            });
            @Language("SQL")
            String sql = "INSERT INTO `" + guildChatDiscordName + "`.`users` (`minecraft_uuid`, `minecraft_name`, `link_code`, `expires_at`) " +
                    "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE `minecraft_name` = VALUES(`minecraft_name`), `link_code` = VALUES(`link_code`), `expires_at` = VALUES(`expires_at`)";
            DatabaseManager.get().query(sql, stmt -> {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, player.getUsername());
                stmt.setString(3, linkCode);
                stmt.setLong(4, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(LINK_EXPIRE_MINUTES));
                stmt.executeUpdate();
            });
            player.sendMessage(VMessages.formatComponent(player, "command.guild.link_discord.link_code", linkCode, LINK_EXPIRE_MINUTES).color(NamedTextColor.GREEN));
        } catch (Exception e) {
            if ("Link code already exists".equals(e.getMessage())) {
                player.sendMessage(VMessages.formatComponent(player, "command.guild.link_discord.generate_code_failed").color(NamedTextColor.RED));
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
        return 0;
    }

    private static int executeUnlinkDiscord(@NotNull Player player) {
        try {
            String guildChatDiscordName = VelocityPlugin.getPlugin().getDatabaseConfig().guildChatDiscordName();
            DatabaseManager.get().query("DELETE FROM `" + guildChatDiscordName + "`.`users` WHERE `minecraft_uuid` = ?", stmt -> {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.executeUpdate();
            });
            player.sendMessage(VMessages.formatComponent(player, "command.guild.unlink_discord.success").color(NamedTextColor.GREEN));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    private static int executeNick(@NotNull Player player, @Nullable String name) {
        long selectedGuild = ensureSelected(player);
        if (selectedGuild == -1) return 0;
        GuildMember member = InterChatProvider.get().getGuildManager().getMember(selectedGuild, player.getUniqueId()).join();
        if ("off".equals(name)) name = null;
        new GuildMember(member.guildId(), member.uuid(), member.role(), name).update();
        if (name == null) {
            player.sendMessage(VMessages.formatComponent(player, "command.guild.nick.off").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(VMessages.formatComponent(player, "command.guild.nick.on", name).color(NamedTextColor.GREEN));
        }
        return 0;
    }

    private static long ensureSelected(@NotNull Player player) {
        long selectedGuild = InterChatProvider.get().getUserManager().fetchUser(player.getUniqueId()).join().selectedGuild();
        if (selectedGuild == -1) {
            player.sendMessage(VMessages.formatComponent(player, "command.guild.not_selected", COMMAND_NAME).color(NamedTextColor.RED));
            return -1;
        }
        try {
            InterChatProvider.get().getGuildManager().getMember(selectedGuild, player.getUniqueId()).join();
        } catch (CompletionException e) {
            // not a member of the guild
            player.sendMessage(VMessages.formatComponent(player, "command.guild.not_selected", COMMAND_NAME).color(NamedTextColor.RED));
            return -1;
        }
        return selectedGuild;
    }
}
