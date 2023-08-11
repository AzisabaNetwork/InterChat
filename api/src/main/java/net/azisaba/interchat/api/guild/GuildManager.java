package net.azisaba.interchat.api.guild;

import net.azisaba.interchat.api.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface GuildManager {
    /**
     * Creates a guild.
     * @param name guild name
     * @param format format of the chat
     * @return guild if successful, empty otherwise
     */
    @NotNull
    CompletableFuture<Optional<Guild>> createGuild(@NotNull String name, @NotNull String format);

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

    @NotNull
    CompletableFuture<GuildBan> createBan(long guildId, @NotNull UUID uuid, @Nullable String reason, boolean reasonPublic);

    @NotNull
    CompletableFuture<Collection<GuildBan>> getBans(long guildId);

    @NotNull
    default CompletableFuture<Collection<GuildBan>> getBans(@NotNull Guild guild) {
        return getBans(guild.id());
    }

    @NotNull
    CompletableFuture<Optional<GuildBan>> getBan(long guildId, @NotNull UUID uuid);

    @NotNull
    default CompletableFuture<Optional<GuildBan>> getBan(@NotNull Guild guild, @NotNull UUID uuid) {
        return getBan(guild.id(), uuid);
    }

    @NotNull
    default CompletableFuture<Optional<GuildBan>> getBan(@NotNull Guild guild, @NotNull User user) {
        return getBan(guild.id(), user.id());
    }

    @NotNull
    CompletableFuture<Void> deleteBan(long guildId, @NotNull UUID uuid);

    @NotNull
    default CompletableFuture<Void> deleteBan(@NotNull GuildBan ban) {
        return deleteBan(ban.guildId(), ban.uuid());
    }
}
