package net.azisaba.interchat.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.network.Protocol;
import net.azisaba.interchat.api.network.protocol.GuildMessagePacket;
import net.azisaba.interchat.api.text.KanaTranslator;
import net.azisaba.interchat.velocity.VelocityPlugin;
import net.azisaba.interchat.velocity.command.GuildCommand;
import net.azisaba.interchat.velocity.database.DatabaseManager;
import net.azisaba.interchat.velocity.text.VMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
                    InterChatProvider.get().getGuildManager().getMember(focusedGuild, uuid).join();
                } catch (CompletionException ex) {
                    // focused but not in guild
                    DatabaseManager.get().query("UPDATE `players` SET `focused_guild` = -1 WHERE `id` = ?", stmt -> {
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

    @Subscribe(order = PostOrder.LATE)
    public void onPlayerChat(PlayerChatEvent e) {
        if (!e.getResult().isAllowed()) {
            // not allowed to chat due to other plugins
            return;
        }
        String message = e.getMessage();
        if (message.startsWith("/")) return; // don't process commands
        long focusedGuildId = getFocusedGuildId(e.getPlayer().getUniqueId());
        if (focusedGuildId == -1) return; // no focused guild
        if (e.getPlayer().getProtocolVersion().ordinal() >= ProtocolVersion.valueOf("MINECRAFT_1_19_1").ordinal()) {
            // See comments in body of GuildCommand#executeSetFocusedGuild
            e.setResult(PlayerChatEvent.ChatResult.denied());
            e.getPlayer().disconnect(VMessages.formatComponent(e.getPlayer(), "generic.1_19_1_not_supported")
                    .color(NamedTextColor.RED)
                    .append(Component.newline())
                    .append(VMessages.formatComponent(e.getPlayer(), "guild.focus.kick_message", GuildCommand.COMMAND_NAME).color(NamedTextColor.RED)));
            return;
        }
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
        if (message.startsWith("!")) {
            // if the message starts with !, send to the backend without the first !
            e.setResult(PlayerChatEvent.ChatResult.message(message.substring(1)));
            return;
        }
        e.setResult(PlayerChatEvent.ChatResult.denied());

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
                e.getPlayer().getCurrentServer().orElseThrow(IllegalStateException::new).getServerInfo().getName(),
                e.getPlayer().getUniqueId(),
                message,
                transliteratedMessage);
        VelocityPlugin.getPlugin().getJedisBox().getPubSubHandler().publish(Protocol.GUILD_MESSAGE.getName(), packet);
    }
}
