package net.azisaba.interchat.velocity.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import net.azisaba.interchat.api.Logger;
import net.azisaba.interchat.api.guild.Guild;
import net.azisaba.interchat.velocity.command.argument.GuildArgumentType;
import net.azisaba.interchat.velocity.database.DatabaseManager;
import net.azisaba.interchat.velocity.text.VMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class GuildAdminCommand extends AbstractCommand {
    @Override
    protected @NotNull LiteralArgumentBuilder<CommandSource> createBuilder() {
        return literal("guildadmin")
                .requires(source -> source.hasPermission("interchat.guildadmin"))
                .then(literal("guild")
                        .requires(source -> source.hasPermission("interchat.guildadmin.guild"))
                        .then(argument("guild", GuildArgumentType.guild())
                                .then(literal("soft-delete")
                                        .requires(source -> source.hasPermission("interchat.guildadmin.guild.soft-delete"))
                                )
                                .then(literal("hard-delete")
                                        .requires(source -> source.hasPermission("interchat.guildadmin.guild.hard-delete"))
                                        .then(argument("code", StringArgumentType.greedyString())
                                        )
                                )
                                .then(literal("restore")
                                        .requires(source -> source.hasPermission("interchat.guildadmin.guild.restore"))
                                        .executes(ctx -> executeGuildRestore(ctx.getSource(), GuildArgumentType.get(ctx, "guild", true)))
                                )
                        )
                );
    }

    private static int executeGuildRestore(@NotNull CommandSource source, @NotNull Guild guild) {
        try {
            DatabaseManager.get().runPrepareStatement("UPDATE `guilds` SET `deleted` = 0 WHERE `id` = ?", stmt -> {
                stmt.setLong(1, guild.id());
                stmt.executeUpdate();
            });
            DatabaseManager.get().submitLog(guild.id(), source, "Restored guild");
            source.sendMessage(Component.text(VMessages.format(source, "command.guildadmin.guild.restore.success"), NamedTextColor.GREEN));
        } catch (SQLException e) {
            Logger.getCurrentLogger().error("Failed to restore the guild {}", guild.id(), e);
            source.sendMessage(Component.text(VMessages.format(source, "command.guildadmin.guild.restore.error"), NamedTextColor.RED));
        }
        return 0;
    }
}
