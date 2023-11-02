package net.azisaba.interchat.velocity.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

public class GuildTestCommand extends AbstractCommand {
    private static final Command<CommandSource> COMMAND = ctx -> {
        ctx.getSource().sendMessage(
                Component.text("/guild_testは改名され、/guildになりました！", NamedTextColor.YELLOW)
        );
        return 0;
    };

    @Override
    protected @NotNull LiteralArgumentBuilder<CommandSource> createBuilder() {
        return literal("guild_test")
                .executes(COMMAND)
                .then(literal("info").executes(COMMAND));
    }
}
