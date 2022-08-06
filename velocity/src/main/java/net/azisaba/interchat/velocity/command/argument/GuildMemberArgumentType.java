package net.azisaba.interchat.velocity.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.guild.GuildMember;
import net.azisaba.interchat.api.guild.GuildRole;
import net.azisaba.interchat.api.user.User;
import net.azisaba.interchat.velocity.text.VMessages;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.Function;

public class GuildMemberArgumentType {
    private static final SimpleCommandExceptionType EXPECTED_PLAYER = new SimpleCommandExceptionType(new LiteralMessage("Expected player"));
    private static final SimpleCommandExceptionType STATIC_INVALID_GUILD_MEMBER = new SimpleCommandExceptionType(new LiteralMessage("Invalid guild member"));
    private static final BiFunction<CommandContext<CommandSource>, Object, CommandSyntaxException> INVALID_GUILD_MEMBER = (ctx, o) -> {
        String formatted = VMessages.format(ctx.getSource(), "command.error.unknown_guild_member", o);
        return new CommandSyntaxException(STATIC_INVALID_GUILD_MEMBER, new LiteralMessage(formatted));
    };
    private static final SimpleCommandExceptionType STATIC_GUILD_NOT_SELECTED = new SimpleCommandExceptionType(new LiteralMessage("Guild not selected"));
    private static final Function<CommandSource, CommandSyntaxException> GUILD_NOT_SELECTED = (source) -> {
        String formatted = VMessages.format(source, "command.guild.not_selected");
        return new CommandSyntaxException(STATIC_GUILD_NOT_SELECTED, new LiteralMessage(formatted));
    };
    private static final SimpleCommandExceptionType STATIC_INSUFFICIENT_PERMISSION = new SimpleCommandExceptionType(new LiteralMessage("You do not have permission to do this."));
    private static final Function<CommandSource, CommandSyntaxException> INSUFFICIENT_PERMISSION = (source) -> {
        String formatted = VMessages.format(source, "command.error.permission");
        return new CommandSyntaxException(STATIC_INSUFFICIENT_PERMISSION, new LiteralMessage(formatted));
    };

    @Contract(pure = true)
    public static @NotNull StringArgumentType guildMember() {
        return StringArgumentType.string();
    }

    @Blocking
    public static @NotNull GuildMember get(@NotNull CommandContext<CommandSource> ctx, @NotNull String name) throws CommandSyntaxException {
        return get(ctx, name, null);
    }

    @Blocking
    public static @NotNull GuildMember get(@NotNull CommandContext<CommandSource> ctx, @NotNull String name, @Nullable GuildRole requiredRole) throws CommandSyntaxException {
        if (!(ctx.getSource() instanceof Player player)) {
            throw EXPECTED_PLAYER.create();
        }
        long selectedGuild = InterChatProvider.get().getUserManager().fetchUser(player.getUniqueId()).join().selectedGuild();
        if (selectedGuild == -1) {
            throw GUILD_NOT_SELECTED.apply(player);
        }
        if (requiredRole != null) {
            GuildMember self = InterChatProvider.get().getGuildManager().getMember(selectedGuild, player.getUniqueId()).join();
            if (requiredRole.ordinal() < self.role().ordinal()) {
                throw INSUFFICIENT_PERMISSION.apply(player);
            }
        }
        String value = StringArgumentType.getString(ctx, name);
        UUID uuid;
        try {
            uuid = UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            List<User> users = InterChatProvider.get().getUserManager().fetchUserByUsername(value).join();
            if (users.isEmpty()) {
                throw INVALID_GUILD_MEMBER.apply(ctx, value);
            } else if (users.size() > 1) {
                throw INVALID_GUILD_MEMBER.apply(ctx, value);
            } else {
                uuid = users.get(0).id();
            }
        }
        try {
            return InterChatProvider.get()
                    .getGuildManager()
                    .getMember(selectedGuild, uuid)
                    .join();
        } catch (NoSuchElementException e) {
            throw INVALID_GUILD_MEMBER.apply(ctx, value);
        } catch (CompletionException e) {
            if (e.getCause() instanceof NoSuchElementException) {
                throw INVALID_GUILD_MEMBER.apply(ctx, value);
            }
        }
        throw new NoSuchElementException(name);
    }
}
