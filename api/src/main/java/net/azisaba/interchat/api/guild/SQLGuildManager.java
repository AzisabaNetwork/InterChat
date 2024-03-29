package net.azisaba.interchat.api.guild;

import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.user.User;
import net.azisaba.interchat.api.util.QueryExecutor;
import net.azisaba.interchat.api.util.ResultSetUtil;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SQLGuildManager implements GuildManager {
    protected final QueryExecutor queryExecutor;

    public SQLGuildManager(@NotNull QueryExecutor queryExecutor) {
        this.queryExecutor = Objects.requireNonNull(queryExecutor, "queryExecutor");
    }

    @Override
    public @NotNull CompletableFuture<Optional<Guild>> createGuild(@NotNull String name, @NotNull String format) {
        CompletableFuture<Optional<Guild>> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                queryExecutor.queryWithGeneratedKeys("INSERT INTO `guilds` (`name`, `format`) VALUES (?, ?)", stmt -> {
                    stmt.setString(1, name);
                    stmt.setString(2, format);
                    if (stmt.executeUpdate() == 0) {
                        future.complete(Optional.empty());
                    }
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            future.complete(Optional.of(fetchGuildById(keys.getLong(1)).join()));
                        } else {
                            future.complete(Optional.empty());
                        }
                    }
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public @NotNull CompletableFuture<Guild> fetchGuildById(long id) {
        CompletableFuture<Guild> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                queryExecutor.query("SELECT * FROM `guilds` WHERE `id` = ? LIMIT 1", stmt -> {
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
                queryExecutor.query("SELECT * FROM `guilds` WHERE `name` = ? LIMIT 1", stmt -> {
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
                queryExecutor.query("SELECT * FROM `guild_members` WHERE `guild_id` = ?", stmt -> {
                    stmt.setLong(1, guildId);
                    List<GuildMember> members = ResultSetUtil.toList(stmt.executeQuery(), GuildMember::createByResultSet);
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
                queryExecutor.query("SELECT * FROM `guild_members` WHERE `guild_id` = ? AND `uuid` = ? LIMIT 1", stmt ->{
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
                queryExecutor.query("DELETE FROM `guild_members` WHERE `guild_id` = ? AND `uuid` = ? LIMIT 1", stmt ->{
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
    public @NotNull CompletableFuture<Void> updateMemberRole(long guildId, @NotNull UUID uuid, @NotNull GuildRole role) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                queryExecutor.query("INSERT INTO `guild_members` (`guild_id`, `uuid`, `role`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `role` = VALUES(`role`)", stmt ->{
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

    @Override
    public @NotNull CompletableFuture<Void> updateMember(@NotNull GuildMember member) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                queryExecutor.query("INSERT INTO `guild_members` (`guild_id`, `uuid`, `role`, `nickname`, `hidden_by_member`) VALUES (?, ?, ?, ?, ?)" +
                        " ON DUPLICATE KEY UPDATE `role` = VALUES(`role`), `nickname` = VALUES(`nickname`), `hidden_by_member` = VALUES(`hidden_by_member`)", stmt ->{
                    stmt.setLong(1, member.guildId());
                    stmt.setString(2, member.uuid().toString());
                    stmt.setString(3, member.role().name());
                    stmt.setString(4, member.nickname());
                    stmt.setBoolean(5, member.hiddenByMember());
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
                queryExecutor.query("SELECT `guilds`.* FROM `guild_members` LEFT JOIN `guilds` ON `guilds`.`id` = `guild_members`.`guild_id` WHERE `guild_members`.`uuid` = ?", stmt ->{
                    stmt.setString(1, uuid.toString());
                    List<Guild> guilds = ResultSetUtil.toList(stmt.executeQuery(), Guild::createByResultSet);
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
                queryExecutor.query(query, stmt ->{
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, GuildRole.OWNER.name());
                    List<Guild> guilds = ResultSetUtil.toList(stmt.executeQuery(), Guild::createByResultSet);
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
                queryExecutor.query("SELECT * FROM `guild_invites` WHERE `guild_id` = ? AND `target` = ?", stmt ->{
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
                queryExecutor.query("DELETE FROM `guild_invites` WHERE `guild_id` = ? AND `target` = ?", stmt ->{
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

    @Override
    public @NotNull CompletableFuture<GuildBan> createBan(long guildId, @NotNull UUID uuid, @Nullable String reason, boolean reasonPublic) {
        CompletableFuture<GuildBan> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                queryExecutor.query("INSERT INTO `guild_bans` (`guild_id`, `uuid`, `reason`, `reason_public`) VALUES (?, ?, ?, ?)", stmt -> {
                    stmt.setLong(1, guildId);
                    stmt.setString(2, uuid.toString());
                    stmt.setString(3, reason);
                    stmt.setBoolean(4, reasonPublic);
                    stmt.executeUpdate();
                    future.complete(new GuildBan(guildId, uuid, reason, reasonPublic));
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public @NotNull CompletableFuture<Collection<GuildBan>> getBans(long guildId) {
        CompletableFuture<Collection<GuildBan>> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                Set<GuildBan> bans = new HashSet<>();
                queryExecutor.query("SELECT * FROM `guild_bans` WHERE `guild_id` = ?", stmt -> {
                    stmt.setLong(1, guildId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            bans.add(GuildBan.createByResultSet(rs));
                        }
                    }
                });
                future.complete(bans);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public @NotNull CompletableFuture<Optional<GuildBan>> getBan(long guildId, @NotNull UUID uuid) {
        CompletableFuture<Optional<GuildBan>> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                queryExecutor.query("SELECT * FROM `guild_bans` WHERE `guild_id` = ? AND `uuid` = ? LIMIT 1", stmt -> {
                    stmt.setLong(1, guildId);
                    stmt.setString(2, uuid.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            future.complete(Optional.of(GuildBan.createByResultSet(rs)));
                        } else {
                            future.complete(Optional.empty());
                        }
                    }
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public @NotNull CompletableFuture<Void> deleteBan(long guildId, @NotNull UUID uuid) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                queryExecutor.query("DELETE FROM `guild_bans` WHERE `guild_id` = ? AND `uuid` = ? LIMIT 1", stmt -> {
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
}
