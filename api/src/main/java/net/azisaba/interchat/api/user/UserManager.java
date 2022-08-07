package net.azisaba.interchat.api.user;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserManager {
    @NotNull
    CompletableFuture<User> fetchUser(@NotNull UUID uuid);

    // might include different users with the same name but different uuid
    @NotNull
    CompletableFuture<List<User>> fetchUserByUsername(@NotNull String username);

    @NotNull
    CompletableFuture<List<User>> fetchUsers(@NotNull Collection<? extends UUID> iterable);
}
