package net.azisaba.interchat.velocity.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.guild.GuildRole;
import net.azisaba.interchat.api.network.Protocol;
import net.azisaba.interchat.api.network.protocol.ProxyboundGuildMessagePacket;
import net.azisaba.interchat.velocity.VelocityPlugin;
import net.azisaba.interchat.velocity.database.DatabaseManager;
import net.azisaba.interchat.velocity.text.VMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CompletionException;

public class GuildCommand extends AbstractCommand {
    private static final String COMMAND_NAME = "guild_test";

    @Override
    public @NotNull LiteralArgumentBuilder<CommandSource> createBuilder() {
        return literal(COMMAND_NAME)
                .then(literal("create")
                        .requires(source -> source instanceof Player)
                        .then(argument("name", StringArgumentType.word())
                                .executes(ctx -> executeCreate((Player) ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                )
                .then(literal("chat")
                        .requires(source -> source instanceof Player)
                        .then(argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> executeChat((Player) ctx.getSource(), StringArgumentType.getString(ctx, "message")))
                        )
                );
    }

    private static int executeCreate(@NotNull Player player, @NotNull String name) {
        try {
            InterChatProvider.get().getGuildManager().fetchGuildByName(name).join();
            player.sendMessage(Component.text(VMessages.format(player, "command.guild.create.already_exists"), NamedTextColor.RED));
            return 0;
        } catch (CompletionException ignore) {
        }
        try {
            DatabaseManager db = DatabaseManager.get();
            long guildId;
            // create guild
            try (
                    Connection connection = db.getConnection();
                    PreparedStatement stmt = connection.prepareStatement("INSERT INTO `guilds` (`name`) VALUES (?)", Statement.RETURN_GENERATED_KEYS)
            ) {
                stmt.setString(1, name);
                if (stmt.executeUpdate() == 0) {
                    player.sendMessage(Component.text(VMessages.format(player, "command.guild.create.fail"), NamedTextColor.RED));
                    return 0;
                }
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    guildId = keys.getLong(1);
                } else {
                    player.sendMessage(Component.text(VMessages.format(player, "command.guild.create.fail"), NamedTextColor.RED));
                    return 0;
                }
                keys.close();
            }
            // mark the player as the owner of the guild
            db.runPrepareStatement("INSERT INTO `guild_members` (`guild_id`, `uuid`, `role`) VALUES (?, ?, ?)", stmt -> {
                stmt.setLong(1, guildId);
                stmt.setString(2, player.getUniqueId().toString());
                stmt.setString(3, GuildRole.OWNER.name());
                stmt.executeUpdate();
            });
            // select the guild
            db.runPrepareStatement("UPDATE `players` SET `selected_guild` = ? WHERE `id` = ?", stmt -> {
                stmt.setLong(1, guildId);
                stmt.setString(2, player.getUniqueId().toString());
                stmt.executeUpdate();
            });
            player.sendMessage(Component.text(VMessages.format(player, "command.guild.create.success"), NamedTextColor.GREEN));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    private static int executeChat(@NotNull Player player, @NotNull String message) {
        long selectedGuild = InterChatProvider.get().getUserManager().fetchUser(player.getUniqueId()).join().selectedGuild();
        if (selectedGuild == -1) {
            player.sendMessage(Component.text(VMessages.format(player, "command.guild.not_selected", COMMAND_NAME), NamedTextColor.RED));
            return 0;
        }
        ProxyboundGuildMessagePacket packet = new ProxyboundGuildMessagePacket(
                selectedGuild,
                player.getCurrentServer().orElseThrow(IllegalStateException::new).getServerInfo().getName(),
                player.getUniqueId(),
                message);
        VelocityPlugin.getPlugin().getJedisBox().getPubSubHandler().publish(Protocol.P_GUILD_MESSAGE_PACKET.getName(), packet);
        return 0;
    }
}
