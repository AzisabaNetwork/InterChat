package net.azisaba.interchat.velocity.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import org.jetbrains.annotations.NotNull;

public class GuildAdminCommand extends AbstractCommand {
    @Override
    protected @NotNull LiteralArgumentBuilder<CommandSource> createBuilder() {
        return literal("guildadmin")
                .requires(source -> source.hasPermission("interchat.guildadmin"))
                .then(literal("guild")

                );
    }
}
