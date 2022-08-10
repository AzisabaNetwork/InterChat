package net.azisaba.interchat.velocity.guild;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.guild.Guild;
import net.azisaba.interchat.api.guild.GuildInvite;
import net.azisaba.interchat.api.guild.GuildManager;
import net.azisaba.interchat.api.guild.GuildMember;
import net.azisaba.interchat.api.guild.GuildRole;
import net.azisaba.interchat.api.network.Protocol;
import net.azisaba.interchat.api.network.protocol.GuildSoftDeletePacket;
import net.azisaba.interchat.api.user.User;
import net.azisaba.interchat.api.util.ResultSetUtil;
import net.azisaba.interchat.velocity.VelocityPlugin;
import net.azisaba.interchat.velocity.database.DatabaseManager;
import net.azisaba.interchat.velocity.listener.ChatListener;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class VelocityGuildManager implements GuildManager {
    public static void markDeleted(@NotNull CommandSource source, long guildId) {
        try {
            DatabaseManager.get().runPrepareStatement("UPDATE `guilds` SET `deleted` = 1 WHERE `id` = ?", stmt -> {
                stmt.setLong(1, guildId);
                stmt.executeUpdate();
            });
            DatabaseManager.get().submitLog(guildId, source, "Deleted guild (soft)");
            DatabaseManager.get().runPrepareStatement("UPDATE `players` SET `selected_guild` = -1 WHERE `selected_guild` = ?", stmt -> {
                stmt.setLong(1, guildId);
                stmt.executeUpdate();
            });
            DatabaseManager.get().runPrepareStatement("UPDATE `players` SET `focused_guild` = -1 WHERE `focused_guild` = ?", stmt -> {
                stmt.setLong(1, guildId);
                stmt.executeUpdate();
            });
            ChatListener.removeCacheWithGuildId(guildId);
            // notify others
            UUID uuid;
            if (source instanceof Player) {
                uuid = ((Player) source).getUniqueId();
            } else {
                uuid = new UUID(0, 0);
            }
            GuildSoftDeletePacket packet = new GuildSoftDeletePacket(guildId, uuid);
            VelocityPlugin.getPlugin().getJedisBox().getPubSubHandler().publish(Protocol.GUILD_SOFT_DELETE.getName(), packet);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public @NotNull CompletableFuture<Guild> fetchGuildById(long id) {
        CompletableFuture<Guild> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                DatabaseManager.get().runPrepareStatement("SELECT * FROM `guilds` WHERE `id` = ? LIMIT 1", stmt -> {
                    stmt.setLong(1, id);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        future.complete(Guild.createByResultSet(rs));
                    } else {
                        future.completeExceptionally(new NoSuchElementException("No guild found with id " + id));
                    }
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public @NotNull CompletableFuture<Guild> fetchGuildByName(@NotNull String name) {
        CompletableFuture<Guild> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                DatabaseManager.get().runPrepareStatement("SELECT * FROM `guilds` WHERE `name` = ? LIMIT 1", stmt -> {
                    stmt.setString(1, name);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        future.complete(Guild.createByResultSet(rs));
                    } else {
                        future.completeExceptionally(new NoSuchElementException("No guild found with name " + name));
                    }
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public @NotNull CompletableFuture<List<GuildMember>> getMembers(long guildId) {
        CompletableFuture<List<GuildMember>> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                DatabaseManager.get().runPrepareStatement("SELECT * FROM `guild_members` WHERE `guild_id` = ?", stmt ->{
                    stmt.setLong(1, guildId);
                    ResultSet rs = stmt.executeQuery();
                    List<GuildMember> members = ResultSetUtil.toList(rs, GuildMember::createByResultSet);
                    future.complete(members);
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public @NotNull CompletableFuture<List<GuildMember>> getMembers(@NotNull Guild guild) {
        return getMembers(guild.id());
    }

    @Override
    public @NotNull CompletableFuture<GuildMember> getMember(long guildId, @NotNull UUID uuid) {
        CompletableFuture<GuildMember> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                DatabaseManager.get().runPrepareStatement("SELECT * FROM `guild_members` WHERE `guild_id` = ? AND `uuid` = ? LIMIT 1", stmt ->{
                    stmt.setLong(1, guildId);
                    stmt.setString(2, uuid.toString());
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        future.complete(GuildMember.createByResultSet(rs));
                    } else {
                        future.completeExceptionally(new NoSuchElementException("No guild member found with guild id " + guildId + " and uuid " + uuid));
                    }
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public @NotNull CompletableFuture<GuildMember> getMember(@NotNull Guild guild, @NotNull UUID uuid) {
        return getMember(guild.id(), uuid);
    }

    @Override
    public @NotNull CompletableFuture<GuildMember> getMember(@NotNull Guild guild, @NotNull User user) {
        return getMember(guild.id(), user.id());
    }

    @Override
    public @NotNull CompletableFuture<Void> removeMember(long guildId, @NotNull UUID uuid) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                DatabaseManager.get().runPrepareStatement("DELETE FROM `guild_members` WHERE `guild_id` = ? AND `uuid` = ? LIMIT 1", stmt ->{
                    stmt.setLong(1, guildId);
                    stmt.setString(2, uuid.toString());
                    stmt.executeUpdate();
                    future.complete(null);
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * Updates the guild member data. This also can be used to add a new member.
     * @param guildId The guild id
     * @param uuid The uuid of the player (member)
     * @param role The role
     * @return void future
     */
    @Override
    public @NotNull CompletableFuture<Void> updateMemberRole(long guildId, @NotNull UUID uuid, @NotNull GuildRole role) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                DatabaseManager.get().runPrepareStatement("INSERT INTO `guild_members` (`guild_id`, `uuid`, `role`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `role` = VALUES(`role`)", stmt ->{
                    stmt.setLong(1, guildId);
                    stmt.setString(2, uuid.toString());
                    stmt.setString(3, role.name());
                    stmt.executeUpdate();
                    future.complete(null);
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Contract(pure = true)
    @Override
    public @NotNull CompletableFuture<List<Guild>> getGuildsOf(@NotNull UUID uuid) {
        CompletableFuture<List<Guild>> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                DatabaseManager.get().runPrepareStatement("SELECT `guilds`.* FROM `guild_members` LEFT JOIN `guilds` ON `guilds`.`id` = `guild_members`.`guild_id` WHERE `guild_members`.`uuid` = ?", stmt ->{
                    stmt.setString(1, uuid.toString());
                    ResultSet rs = stmt.executeQuery();
                    List<Guild> guilds = ResultSetUtil.toList(rs, Guild::createByResultSet);
                    future.complete(guilds);
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public @NotNull CompletableFuture<List<Guild>> getGuildsOf(@NotNull User user) {
        return getGuildsOf(user.id());
    }

    @Override
    public @NotNull CompletableFuture<List<Guild>> getOwnedGuilds(@NotNull UUID uuid, boolean includeDeleted) {
        CompletableFuture<List<Guild>> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            @Language("SQL")
            String query = "SELECT `guilds`.* FROM `guild_members` LEFT JOIN `guilds` ON `guilds`.`id` = `guild_members`.`guild_id` WHERE `guild_members`.`uuid` = ? AND `guild_members`.`role` = ?";
            if (!includeDeleted) {
                query += " AND `guilds`.`deleted` = 0";
            }
            try {
                DatabaseManager.get().runPrepareStatement(query, stmt ->{
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, GuildRole.OWNER.name());
                    ResultSet rs = stmt.executeQuery();
                    List<Guild> guilds = ResultSetUtil.toList(rs, Guild::createByResultSet);
                    future.complete(guilds);
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public @NotNull CompletableFuture<GuildInvite> getInvite(long guildId, @NotNull UUID uuid) {
        CompletableFuture<GuildInvite> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                DatabaseManager.get().runPrepareStatement("SELECT * FROM `guild_invites` WHERE `guild_id` = ? AND `target` = ?", stmt ->{
                    stmt.setLong(1, guildId);
                    stmt.setString(2, uuid.toString());
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        future.complete(GuildInvite.createFromResultSet(rs));
                    } else {
                        future.completeExceptionally(new NoSuchElementException("No guild member found with guild id " + guildId + " and uuid " + uuid));
                    }
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public @NotNull CompletableFuture<Void> deleteInvite(long guildId, @NotNull UUID uuid) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                DatabaseManager.get().runPrepareStatement("DELETE FROM `guild_invites` WHERE `guild_id` = ? AND `target` = ?", stmt ->{
                    stmt.setLong(1, guildId);
                    stmt.setString(2, uuid.toString());
                    stmt.executeUpdate();
                    future.complete(null);
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public @NotNull CompletableFuture<Void> deleteInvite(@NotNull GuildInvite invite) {
        return deleteInvite(invite.guildId(), invite.target());
    }
}
