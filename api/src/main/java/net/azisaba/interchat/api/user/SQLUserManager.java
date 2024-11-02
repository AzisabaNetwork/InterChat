package net.azisaba.interchat.api.user;

import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.util.QueryExecutor;
import net.azisaba.interchat.api.util.ResultSetUtil;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SQLUserManager implements UserManager {
    protected final QueryExecutor queryExecutor;

    public SQLUserManager(@NotNull QueryExecutor queryExecutor) {
        this.queryExecutor = Objects.requireNonNull(queryExecutor, "queryExecutor");
    }

    @Override
    public @NotNull CompletableFuture<User> fetchUser(@NotNull UUID uuid) {
        CompletableFuture<User> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                queryExecutor.query("SELECT * FROM `players` WHERE `id` = ? LIMIT 1", stmt ->{
                    stmt.setString(1, uuid.toString());
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        future.complete(User.createByResultSet(rs));
                    } else {
                        future.completeExceptionally(new NoSuchElementException("No user found with id " + uuid));
                    }
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public @NotNull CompletableFuture<List<User>> fetchUserByUsername(@NotNull String username) {
        CompletableFuture<List<User>> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                queryExecutor.query("SELECT * FROM `players` WHERE `name` = ?", stmt ->{
                    stmt.setString(1, username);
                    ResultSet rs = stmt.executeQuery();
                    future.complete(ResultSetUtil.toList(rs, User::createByResultSet));
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public @NotNull CompletableFuture<List<User>> fetchUsers(@NotNull Collection<? extends UUID> uuids) {
        if (uuids.isEmpty()) return CompletableFuture.completedFuture(Collections.emptyList());
        CompletableFuture<List<User>> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                queryExecutor.query("SELECT * FROM `players` WHERE `id` IN (" + uuids.stream().map(u -> "?").collect(Collectors.joining(",")) + ")", stmt ->{
                    int i = 0;
                    for (UUID uuid : uuids) {
                        stmt.setString(++i, uuid.toString());
                    }
                    ResultSet rs = stmt.executeQuery();
                    future.complete(ResultSetUtil.toList(rs, User::createByResultSet));
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> isBlocked(@NotNull UUID uuid, @NotNull UUID target) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                queryExecutor.query("SELECT * FROM `blocked_users` WHERE `id` = ? AND `blocked_uuid` = ? LIMIT 1", stmt ->{
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, target.toString());
                    ResultSet rs = stmt.executeQuery();
                    future.complete(rs.next());
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> blockUser(@NotNull UUID uuid, @NotNull UUID target) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                queryExecutor.query("INSERT INTO `blocked_users` (`id`, `blocked_uuid`) VALUES (?, ?)", stmt ->{
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, target.toString());
                    stmt.executeUpdate();
                    future.complete(true);
                });
            } catch (Throwable t) {
                // ignore duplicate key error
                if (t.getMessage().toLowerCase(Locale.ROOT).contains("duplicate entry")) {
                    future.complete(false);
                } else {
                    future.completeExceptionally(t);
                }
            }
        });
        return future;
    }

    @Override
    public @NotNull CompletableFuture<Void> unblockUser(@NotNull UUID uuid, @NotNull UUID target) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                queryExecutor.query("DELETE FROM `blocked_users` WHERE `id` = ? AND `blocked_uuid` = ?", stmt ->{
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, target.toString());
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
    public @NotNull CompletableFuture<List<UUID>> fetchBlockedUsers(@NotNull UUID uuid) {
        CompletableFuture<List<UUID>> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                queryExecutor.query("SELECT * FROM `blocked_users` WHERE `id` = ?", stmt ->{
                    stmt.setString(1, uuid.toString());
                    ResultSet rs = stmt.executeQuery();
                    List<UUID> blockedUsers = new ArrayList<>();
                    while (rs.next()) {
                        blockedUsers.add(UUID.fromString(rs.getString("blocked_uuid")));
                    }
                    future.complete(blockedUsers);
                });
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }
}
