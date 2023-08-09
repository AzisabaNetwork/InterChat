package net.azisaba.interchat.velocity.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.azisaba.interchat.velocity.command.argument.GuildArgumentType;
import org.jetbrains.annotations.NotNull;

public class GSShortCommand extends AbstractCommand {
    @Override
    protected @NotNull LiteralArgumentBuilder<CommandSource> createBuilder() {
        return literal("gs")
                .requires(source -> source instanceof Player && source.hasPermission("interchat.guild") && source.hasPermission("interchat.guild.select"))
                .then(argument("guild", GuildArgumentType.guild())
                        .suggests(suggestGuildsOfMember(false))
                        .executes(ctx -> GuildCommand.executeSelect((Player) ctx.getSource(), GuildArgumentType.get(ctx, "guild", false)))
                        .then(argument("message", StringArgumentType.greedyString())
                                .requires(source -> source.hasPermission("interchat.guild.chat"))
                                .suggests(GuildCommand.getChatSuggestionProvider((ctx, uuid) -> GuildArgumentType.get(ctx, "guild", false)))
                                .executes(ctx -> GuildCommand.executeChat((Player) ctx.getSource(), StringArgumentType.getString(ctx, "message"), GuildArgumentType.get(ctx, "guild", false).id()))
                        )
                );
    }
}
