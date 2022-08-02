package net.azisaba.interchat.api.guild;

import net.azisaba.interchat.api.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
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

    @NotNull
    CompletableFuture<GuildMember> getMember(long guildId, @NotNull UUID uuid);

    @NotNull
    CompletableFuture<GuildMember> getMember(@NotNull Guild guild, @NotNull UUID uuid);

    @NotNull
    CompletableFuture<GuildMember> getMember(@NotNull Guild guild, @NotNull User user);
}
