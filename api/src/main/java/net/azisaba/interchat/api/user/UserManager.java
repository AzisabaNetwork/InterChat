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

    /**
     * Check if the user with the given UUID is blocked by the user with the given target UUID.
     * @param uuid the UUID of the user
     * @param target the UUID of the target user
     * @return a future that completes with true if the user is blocked, false otherwise
     */
    @NotNull
    CompletableFuture<Boolean> isBlocked(@NotNull UUID uuid, @NotNull UUID target);

    /**
     * Block the user with the given UUID from the user with the given target UUID.
     * @param uuid the UUID of the user who blocks the target
     * @param target the UUID of the user who is blocked
     * @return a future that completes with true if the user was successfully blocked, false if the user was already blocked
     */
    @NotNull
    CompletableFuture<Boolean> blockUser(@NotNull UUID uuid, @NotNull UUID target);

    /**
     * Unblock the user with the given UUID from the user with the given target UUID.
     * @param uuid the UUID of the user who unblocks the target
     * @param target the UUID of the user who is unblocked
     * @return a future that completes with null if operation was successful
     */
    @NotNull
    CompletableFuture<Void> unblockUser(@NotNull UUID uuid, @NotNull UUID target);

    /**
     * Get the list of UUIDs of users who are blocked by the user with the given UUID.
     * @param uuid the UUID of the user
     * @return a future that completes with the list of UUIDs of blocked users
     */
    @NotNull
    CompletableFuture<List<UUID>> fetchBlockedUsers(@NotNull UUID uuid);
}
