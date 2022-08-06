package net.azisaba.interchat.velocity.user;

import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.user.User;
import net.azisaba.interchat.api.user.UserManager;
import net.azisaba.interchat.api.util.ResultSetUtil;
import net.azisaba.interchat.velocity.database.DatabaseManager;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VelocityUserManager implements UserManager {
    @Override
    public @NotNull CompletableFuture<User> fetchUser(@NotNull UUID uuid) {
        CompletableFuture<User> future = new CompletableFuture<>();
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                DatabaseManager.get().runPrepareStatement("SELECT * FROM `players` WHERE `id` = ? LIMIT 1", stmt ->{
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
                DatabaseManager.get().runPrepareStatement("SELECT * FROM `players` WHERE `name` = ?", stmt ->{
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
}
