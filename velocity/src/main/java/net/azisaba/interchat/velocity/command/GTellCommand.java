package net.azisaba.interchat.velocity.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.azisaba.interchat.velocity.command.argument.UUIDArgumentType;
import org.jetbrains.annotations.NotNull;

public class GTellCommand extends AbstractCommand {
    @Override
    protected @NotNull LiteralArgumentBuilder<CommandSource> createBuilder() {
        return literal("gtell")
                .requires(source -> source instanceof Player && source.hasPermission("interchat.guild") && source.hasPermission("interchat.guild.tell"))
                .executes(ctx -> GuildCommand.executeSetFocusedGuild((Player) ctx.getSource()))
                .then(argument("player", UUIDArgumentType.uuid())
                        .then(argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> GuildCommand.executeTell((Player) ctx.getSource(), UUIDArgumentType.getPlayerWithAPI(ctx, "player"), StringArgumentType.getString(ctx, "message")))));
    }
}
