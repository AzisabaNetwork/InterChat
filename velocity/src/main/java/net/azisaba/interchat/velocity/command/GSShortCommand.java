package net.azisaba.interchat.velocity.command;

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
                );
    }
}
