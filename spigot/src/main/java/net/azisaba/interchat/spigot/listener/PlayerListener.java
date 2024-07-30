package net.azisaba.interchat.spigot.listener;

import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.Logger;
import net.azisaba.interchat.api.guild.GuildMember;
import net.azisaba.interchat.api.network.Protocol;
import net.azisaba.interchat.api.network.protocol.GuildMessagePacket;
import net.azisaba.interchat.api.text.KanaTranslator;
import net.azisaba.interchat.spigot.SpigotPlugin;
import net.azisaba.interchat.spigot.database.DatabaseManager;
import net.azisaba.interchat.spigot.text.SMessages;
import net.azisaba.interchat.spigot.util.ViaUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
public class PlayerListener implements Listener {
    private final SpigotPlugin plugin;

    public PlayerListener(@NotNull SpigotPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.fetchServer(e.getPlayer());
            plugin.sendSignal(e.getPlayer());
            removeCache(e.getPlayer().getUniqueId());
        }, 20);
    }

    private static final int CHAT_COOLDOWN_TIME = 25; //ms
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

    public static void removeCacheWithGuildId(long guildId) {
        List<UUID> toRemove = new ArrayList<>();
        CACHE.forEach((uuid, entry) -> {
            if (entry.getValue() == guildId) {
                toRemove.add(uuid);
            }
        });
        toRemove.forEach(CACHE::remove);
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
                    InterChatProvider.get().getGuildManager().getMember(focusedGuild, uuid).get(1, TimeUnit.SECONDS);
                } catch (Exception ex) {
                    // focused but not in guild
                    DatabaseManager.get().query("UPDATE `players` SET `focused_guild` = -1 WHERE `id` = ?", stmt -> {
                        stmt.setString(1, uuid.toString());
                        stmt.executeUpdate();
                    });
                    focusedGuild = -1L;
                }
            }
            CACHE.put(uuid, new AbstractMap.SimpleImmutableEntry<>(System.currentTimeMillis() + 1000L * 2, focusedGuild));
            return focusedGuild;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent e) {
        try {
            if (ViaUtil.getPlayerVersion(ViaUtil.getViaAPI(), e.getPlayer().getUniqueId()) < 760) {
                // Ignore if player is not on 1.19.1+
                return;
            }
        } catch (ReflectiveOperationException e2) {
            throw new RuntimeException(e2);
        }
        String message = e.getMessage();
        if (message.startsWith("/")) return; // don't process commands
        long focusedGuildId = getFocusedGuildId(e.getPlayer().getUniqueId());
        if (focusedGuildId == -1) return; // no focused guild
        // check cooldown before executing query
        if (checkChatCooldown(e.getPlayer().getUniqueId())) {
            // silently discard message; cooldown is very short anyway
            e.setCancelled(true);
            return;
        }
        try {
            GuildMember self = InterChatProvider.get().getGuildManager().getMember(focusedGuildId, e.getPlayer().getUniqueId()).join();
            if (self.hiddenByMember()) {
                e.getPlayer().sendMessage(ChatColor.RED + SMessages.format(e.getPlayer(), "generic.not_delivered_hide"));
                e.setCancelled(true);
                return;
            }
        } catch (CompletionException ex) {
            removeCache(e.getPlayer().getUniqueId()); // update cache next time
            return; // not in guild
        }
        if (message.startsWith("!")) {
            // if the message starts with !, skip the event without the first !
            e.setMessage(e.getMessage().substring(1));
            return;
        }
        e.setCancelled(true);

        try {
            long hideAllUntil = DatabaseManager.get().getPrepareStatement("SELECT `hide_all_until` FROM `players` WHERE `id` = ?", ps -> {
                ps.setString(1, e.getPlayer().getUniqueId().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("hide_all_until");
                    } else {
                        return 0L;
                    }
                }
            });
            if (hideAllUntil > System.currentTimeMillis()) {
                e.getPlayer().sendMessage(ChatColor.RED + SMessages.format(e.getPlayer(), "generic.not_delivered_hideall"));
                return;
            }
        } catch (SQLException ex) {
            Logger.getCurrentLogger().warn("Failed to check hide all state", ex);
        }

        String transliteratedMessage = null;
        if (message.startsWith(KanaTranslator.SKIP_CHAR_STRING)) {
            message = message.substring(1);
        } else {
            boolean translateKana = InterChatProvider.get().getUserManager().fetchUser(e.getPlayer().getUniqueId()).join().translateKana();
            if (translateKana) {
                List<String> suggestions = KanaTranslator.translateSync(message);
                if (!suggestions.isEmpty()) {
                    transliteratedMessage = suggestions.get(0);
                }
            }
        }

        GuildMessagePacket packet = new GuildMessagePacket(
                focusedGuildId,
                SpigotPlugin.getInstance().server,
                e.getPlayer().getUniqueId(),
                message,
                transliteratedMessage);
        SpigotPlugin.getInstance().getJedisBox().getPubSubHandler().publish(Protocol.GUILD_MESSAGE.getName(), packet);
    }
}
