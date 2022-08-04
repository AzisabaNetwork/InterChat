package net.azisaba.interchat.velocity.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.azisaba.interchat.velocity.VelocityPlugin;
import net.azisaba.interchat.velocity.text.VMessages;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

public class UUIDArgumentType {
    private static final SimpleCommandExceptionType STATIC_INVALID_UUID = new SimpleCommandExceptionType(new LiteralMessage("Invalid UUID"));
    private static final BiFunction<CommandContext<CommandSource>, Object, CommandSyntaxException> INVALID_UUID = (ctx, o) -> {
        String formatted = VMessages.format(ctx.getSource(), "command.error.invalid_uuid", o);
        return new CommandSyntaxException(STATIC_INVALID_UUID, new LiteralMessage(formatted));
    };

    @Contract(pure = true)
    public static @NotNull StringArgumentType uuid() {
        return StringArgumentType.string();
    }

    public static @NotNull UUID get(@NotNull CommandContext<CommandSource> ctx, @NotNull String name) throws CommandSyntaxException {
        String value = StringArgumentType.getString(ctx, name);
        Optional<Player> opt = VelocityPlugin.getProxyServer().getPlayer(value);
        if (opt.isPresent()) {
            return opt.get().getUniqueId();
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw INVALID_UUID.apply(ctx, value);
        }
    }
}
