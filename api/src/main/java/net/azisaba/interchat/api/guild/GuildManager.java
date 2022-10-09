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

    @NotNull
    CompletableFuture<Void> removeMember(long guildId, @NotNull UUID uuid);

    /**
     * Updates the guild member data. This also can be used to add a new member.
     * @param guildId The guild id
     * @param uuid The uuid of the player (member)
     * @param role The role
     * @return void future
     */
    @NotNull
    CompletableFuture<Void> updateMemberRole(long guildId, @NotNull UUID uuid, @NotNull GuildRole role);

    /**
     * Updates the guild member data. This also can be used to add a new member.
     * @param member The guild member
     * @return void future that completes when the update is done
     */
    @NotNull
    CompletableFuture<Void> updateMember(@NotNull GuildMember member);

    @NotNull
    CompletableFuture<List<Guild>> getGuildsOf(@NotNull UUID uuid);

    @NotNull
    CompletableFuture<List<Guild>> getGuildsOf(@NotNull User user);

    @NotNull
    default CompletableFuture<List<Guild>> getOwnedGuilds(@NotNull UUID uuid) {
        return getOwnedGuilds(uuid, true);
    }

    @NotNull
    CompletableFuture<List<Guild>> getOwnedGuilds(@NotNull UUID uuid, boolean includeDeleted);

    @NotNull
    CompletableFuture<GuildInvite> getInvite(long guildId, @NotNull UUID uuid);

    @NotNull
    CompletableFuture<Void> deleteInvite(long guildId, @NotNull UUID uuid);

    @NotNull
    CompletableFuture<Void> deleteInvite(@NotNull GuildInvite invite);
}
