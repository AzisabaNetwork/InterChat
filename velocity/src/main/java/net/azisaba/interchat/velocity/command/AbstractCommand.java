package net.azisaba.interchat.velocity.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.guild.Guild;
import net.azisaba.interchat.api.guild.GuildMember;
import net.azisaba.interchat.api.guild.GuildRole;
import net.azisaba.interchat.api.util.Functions;
import net.azisaba.interchat.velocity.VelocityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class AbstractCommand {
    @NotNull
    protected abstract LiteralArgumentBuilder<CommandSource> createBuilder();

    @Contract(" -> new")
    @NotNull
    public final BrigadierCommand createCommand() {
        return new BrigadierCommand(createBuilder());
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull LiteralArgumentBuilder<CommandSource> literal(@NotNull String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static <T> @NotNull RequiredArgumentBuilder<CommandSource, T> argument(@NotNull String name, @NotNull ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    @NotNull
    public static <S> String getString(@NotNull CommandContext<S> context, @NotNull String name) {
        return StringArgumentType.getString(context, name);
    }

    @Contract(pure = true)
    public static @NotNull SuggestionProvider<CommandSource> suggestPlayers() {
        return (ctx, builder) -> suggest(VelocityPlugin.getProxyServer().getAllPlayers().stream().map(Player::getUsername), builder);
    }

    private static final Function<UUID, Boolean> HAS_SELECTED_GUILD = Functions.memoize(5000, uuid -> {
        long selectedGuild = InterChatProvider.get().getUserManager().fetchUser(uuid).join().selectedGuild();
        return selectedGuild != -1;
    });

    public static boolean hasSelectedGuild(@NotNull CommandSource source) {
        if (!(source instanceof Player player)) {
            return false;
        }
        return HAS_SELECTED_GUILD.apply(player.getUniqueId());
    }

    private static final BiFunction<UUID, GuildRole, Boolean> HAS_ROLE_IN_SELECTED_GUILD = Functions.memoize(5000, (uuid, role) -> {
        long selectedGuild = InterChatProvider.get().getUserManager().fetchUser(uuid).join().selectedGuild();
        if (selectedGuild == -1) {
            return false;
        }
        try {
            int userRole = InterChatProvider.get()
                    .getGuildManager()
                    .getMember(selectedGuild, uuid)
                    .join()
                    .role()
                    .ordinal();
            return userRole <= role.ordinal();
        } catch (CompletionException e) {
            return false;
        }
    });

    public static boolean hasRoleInSelectedGuild(@NotNull CommandSource source, @NotNull GuildRole role) {
        if (!(source instanceof Player player)) {
            return false;
        }
        return HAS_ROLE_IN_SELECTED_GUILD.apply(player.getUniqueId(), role);
    }

    @Contract(pure = true)
    public static @NotNull SuggestionProvider<CommandSource> suggestServers() {
        return (ctx, builder) ->
                suggest(
                        VelocityPlugin.getProxyServer()
                                .getAllServers()
                                .stream()
                                .map(RegisteredServer::getServerInfo)
                                .map(ServerInfo::getName),
                        builder
                );
    }

    private static final Function<UUID, List<Guild>> SUGGESTED_GUILDS_OF_MEMBER = Functions.memoize(10000, uuid ->
            InterChatProvider.get()
                .getGuildManager()
                .getGuildsOf(uuid)
                .join()
    );

    @Contract(pure = true)
    public static @NotNull SuggestionProvider<CommandSource> suggestGuildsOfMember(boolean includeDeleted) {
        return (ctx, builder) -> {
            UUID uuid = ((Player) ctx.getSource()).getUniqueId();
            return suggest(
                    SUGGESTED_GUILDS_OF_MEMBER.apply(uuid)
                            .stream()
                            .filter(guild -> includeDeleted || !guild.deleted())
                            .map(Guild::name),
                    builder
            );
        };
    }

    private static final Function<Long, List<GuildMember>> SUGGESTED_MEMBERS_OF_GUILD = Functions.memoize(10000, guildId ->
            InterChatProvider.get()
                    .getGuildManager()
                    .getMembers(guildId)
                    .join()
    );

    private static final Function<UUID, String> UUID_TO_USERNAME = Functions.memoize(1000 * 60 * 60, uuid ->
            InterChatProvider.get()
                    .getUserManager()
                    .fetchUser(uuid)
                    .join()
                    .name()
    );

    @Contract(pure = true)
    public static @NotNull SuggestionProvider<CommandSource> suggestMembersOfGuild(@Nullable GuildRole requiredRole) {
        return (ctx, builder) -> {
            if (requiredRole != null && !hasRoleInSelectedGuild(ctx.getSource(), requiredRole)) {
                return Suggestions.empty();
            }
            UUID uuid = ((Player) ctx.getSource()).getUniqueId();
            long guildId = InterChatProvider.get()
                    .getUserManager()
                    .fetchUser(uuid)
                    .join()
                    .selectedGuild();
            if (guildId == -1) {
                return Suggestions.empty();
            }
            return suggest(
                    SUGGESTED_MEMBERS_OF_GUILD.apply(guildId)
                            .parallelStream()
                            .map(GuildMember::uuid)
                            .map(UUID_TO_USERNAME),
                    builder
            );
        };
    }

    @NotNull
    public static CompletableFuture<Suggestions> suggest(@NotNull Stream<String> suggestions, @NotNull SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase(Locale.ROOT);
        suggestions
                .filter((suggestion) -> matchesSubStr(input, suggestion.toLowerCase(Locale.ROOT)))
                .sequential()
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    public static boolean matchesSubStr(@NotNull String input, @NotNull String suggestion) {
        for(int i = 0; !suggestion.startsWith(input, i); ++i) {
            i = suggestion.indexOf('_', i);
            if (i < 0) {
                return false;
            }
        }
        return true;
    }

    public static int sendMessageMissingPlayer(@NotNull CommandSource source, @Nullable String playerName) {
        source.sendMessage(Component.text("Player not found: " + playerName).color(NamedTextColor.RED));
        return 0;
    }

    public static int sendMessageMissingServer(@NotNull CommandSource source, @Nullable String serverName) {
        source.sendMessage(Component.text("Server not found: " + serverName).color(NamedTextColor.RED));
        return 0;
    }
}
