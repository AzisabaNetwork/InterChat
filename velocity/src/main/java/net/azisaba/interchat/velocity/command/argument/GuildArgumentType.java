package net.azisaba.interchat.velocity.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.velocitypowered.api.command.CommandSource;
import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.guild.Guild;
import net.azisaba.interchat.velocity.text.VMessages;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;

public class GuildArgumentType {
    private static final SimpleCommandExceptionType STATIC_INVALID_GUILD = new SimpleCommandExceptionType(new LiteralMessage("Invalid guild"));
    private static final BiFunction<CommandContext<CommandSource>, Object, CommandSyntaxException> INVALID_GUILD = (ctx, o) -> {
        String formatted = VMessages.format(ctx.getSource(), "command.error.unknown_guild", o);
        return new CommandSyntaxException(STATIC_INVALID_GUILD, new LiteralMessage(formatted));
    };

    @Contract(pure = true)
    public static @NotNull StringArgumentType guild() {
        return StringArgumentType.string();
    }

    @Blocking
    public static @NotNull Guild get(@NotNull CommandContext<CommandSource> ctx, @NotNull String name, boolean includeDeleted) throws CommandSyntaxException {
        String guildName = StringArgumentType.getString(ctx, name);
        try {
            Guild guild = InterChatProvider.get()
                    .getGuildManager()
                    .fetchGuildByName(guildName)
                    .join();
            if (guild.deleted() && !includeDeleted) {
                throw INVALID_GUILD.apply(ctx, guildName);
            }
            return guild;
        } catch (NoSuchElementException e) {
            throw INVALID_GUILD.apply(ctx, guildName);
        } catch (CompletionException e) {
            if (e.getCause() instanceof NoSuchElementException) {
                throw INVALID_GUILD.apply(ctx, guildName);
            }
        }
        throw new NoSuchElementException(name);
    }
}
