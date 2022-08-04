package net.azisaba.interchat.velocity.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.velocitypowered.api.command.CommandSource;
import net.azisaba.interchat.api.guild.GuildRole;
import net.azisaba.interchat.velocity.text.VMessages;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.function.BiFunction;

public class GuildRoleArgumentType {
    private static final SimpleCommandExceptionType STATIC_INVALID_GUILD_ROLE = new SimpleCommandExceptionType(new LiteralMessage("Invalid guild role"));
    private static final BiFunction<CommandContext<CommandSource>, Object, CommandSyntaxException> INVALID_GUILD_ROLE = (ctx, o) -> {
        String formatted = VMessages.format(ctx.getSource(), "command.error.invalid_guild_role", o);
        return new CommandSyntaxException(STATIC_INVALID_GUILD_ROLE, new LiteralMessage(formatted));
    };

    @Contract(pure = true)
    public static @NotNull StringArgumentType guildRole() {
        return StringArgumentType.string();
    }

    public static @NotNull GuildRole get(@NotNull CommandContext<CommandSource> ctx, @NotNull String name) throws CommandSyntaxException {
        String value = StringArgumentType.getString(ctx, name);
        try {
            return GuildRole.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw INVALID_GUILD_ROLE.apply(ctx, value);
        }
    }
}
