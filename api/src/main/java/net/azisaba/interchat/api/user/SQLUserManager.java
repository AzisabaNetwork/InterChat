package net.azisaba.interchat.api.user;

import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.util.QueryExecutor;
import net.azisaba.interchat.api.util.ResultSetUtil;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
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
}
