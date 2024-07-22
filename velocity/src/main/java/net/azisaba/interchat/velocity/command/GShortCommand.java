package net.azisaba.interchat.velocity.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.azisaba.interchat.velocity.VelocityPlugin;
import org.jetbrains.annotations.NotNull;

public class GShortCommand extends AbstractCommand {
    private final VelocityPlugin plugin;

    public GShortCommand(@NotNull VelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected @NotNull LiteralArgumentBuilder<CommandSource> createBuilder() {
        return literal("g")
                .requires(source -> source instanceof Player && source.hasPermission("interchat.guild") && source.hasPermission("interchat.guild.chat"))
                .executes(ctx -> GuildCommand.executeSetFocusedGuild((Player) ctx.getSource()))
                .then(argument("message", StringArgumentType.greedyString())
                        .suggests(GuildCommand.getChatSuggestionProvider(plugin.getJedisBox(), (ctx, uuid) -> GuildCommand.ACTUAL_GUILD.apply(uuid)))
                        .executes(ctx -> GuildCommand.executeChat((Player) ctx.getSource(), StringArgumentType.getString(ctx, "message")))
                );
    }
}
