package net.azisaba.interchat.velocity.guild;

import net.azisaba.interchat.api.guild.GuildManager;
import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.guild.Guild;
import net.azisaba.interchat.api.guild.GuildMember;
import net.azisaba.interchat.api.util.ResultSetUtil;
import net.azisaba.interchat.velocity.database.DatabaseManager;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

public final class VelocityGuildManager implements GuildManager {
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
}
