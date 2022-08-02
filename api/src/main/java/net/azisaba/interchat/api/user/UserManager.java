package net.azisaba.interchat.api.user;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserManager {
    @NotNull
    CompletableFuture<User> fetchUser(@NotNull UUID uuid);
}
