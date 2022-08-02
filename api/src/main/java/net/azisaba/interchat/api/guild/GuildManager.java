package net.azisaba.interchat.api.guild;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface GuildManager {
    @NotNull
    CompletableFuture<Guild> fetchGuildById(long id);

    @NotNull
    CompletableFuture<Guild> fetchGuildByName(@NotNull String name);

    @NotNull
    CompletableFuture<List<GuildMember>> getMembers(long guildId);

    @NotNull
    CompletableFuture<List<GuildMember>> getMembers(@NotNull Guild guild);
}
