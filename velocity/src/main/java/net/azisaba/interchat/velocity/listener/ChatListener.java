package net.azisaba.interchat.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.network.Protocol;
import net.azisaba.interchat.api.network.protocol.GuildMessagePacket;
import net.azisaba.interchat.velocity.VelocityPlugin;
import net.azisaba.interchat.velocity.database.DatabaseManager;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatListener {
    private static final int CHAT_COOLDOWN_TIME = 300; //ms
    private static final Map<UUID, Long> CHAT_COOLDOWN = new ConcurrentHashMap<>();
    // (player uuid, (expiration time, guild id))
    private static final Map<UUID, Map.Entry<Long, Long>> CACHE = new ConcurrentHashMap<>();

    // true if caller should cancel the chat
    public static boolean checkChatCooldown(@NotNull UUID uuid) {
        if (CHAT_COOLDOWN.containsKey(uuid)) {
            long expiresAt = CHAT_COOLDOWN.get(uuid);
            if (expiresAt > System.currentTimeMillis()) {
                return true;
            }
        }
        CHAT_COOLDOWN.put(uuid, System.currentTimeMillis() + CHAT_COOLDOWN_TIME);
        return false;
    }

    public static void removeCache(@NotNull UUID uuid) {
        CACHE.remove(uuid);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    @Blocking
    public static long getFocusedGuildId(@NotNull UUID uuid) {
        Map.Entry<Long, Long> entry = CACHE.get(uuid);
        if (entry != null) {
            if (entry.getKey() > System.currentTimeMillis()) {
                return entry.getValue();
            }
        }
        try {
            long focusedGuild = DatabaseManager.get().getPrepareStatement("SELECT `focused_guild` FROM `players` WHERE `id` = ?", stmt -> {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    } else {
                        return -1L;
                    }
                }
            });
            if (focusedGuild != -1) {
                try {
                    InterChatProvider.get().getGuildManager().getMember(focusedGuild, uuid).join();
                } catch (CompletionException ex) {
                    // focused but not in guild
                    DatabaseManager.get().runPrepareStatement("UPDATE `players` SET `focused_guild` = -1 WHERE `id` = ?", stmt -> {
                        stmt.setString(1, uuid.toString());
                        stmt.executeUpdate();
                    });
                    focusedGuild = -1L;
                }
            }
            CACHE.put(uuid, new AbstractMap.SimpleImmutableEntry<>(System.currentTimeMillis() + 1000L * 60L * 60L, focusedGuild));
            return focusedGuild;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent e) {
        removeCache(e.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent e) {
        String message = e.getMessage();
        if (message.startsWith("/")) return; // don't process commands
        long focusedGuildId = getFocusedGuildId(e.getPlayer().getUniqueId());
        if (focusedGuildId == -1) return; // no focused guild
        // check cooldown before executing query
        if (checkChatCooldown(e.getPlayer().getUniqueId())) {
            // silently discard message; cooldown is very short anyway
            e.setResult(PlayerChatEvent.ChatResult.denied());
            return;
        }
        try {
            InterChatProvider.get().getGuildManager().getMember(focusedGuildId, e.getPlayer().getUniqueId()).join();
        } catch (CompletionException ex) {
            removeCache(e.getPlayer().getUniqueId()); // update cache next time
            return; // not in guild
        }
        e.setResult(PlayerChatEvent.ChatResult.denied());
        GuildMessagePacket packet = new GuildMessagePacket(
                focusedGuildId,
                e.getPlayer().getCurrentServer().orElseThrow(IllegalStateException::new).getServerInfo().getName(),
                e.getPlayer().getUniqueId(),
                message);
        VelocityPlugin.getPlugin().getJedisBox().getPubSubHandler().publish(Protocol.GUILD_MESSAGE.getName(), packet);
    }
}