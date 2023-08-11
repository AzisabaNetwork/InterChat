package net.azisaba.interchat.velocity.guild;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.azisaba.interchat.api.Logger;
import net.azisaba.interchat.api.guild.GuildManager;
import net.azisaba.interchat.api.guild.SQLGuildManager;
import net.azisaba.interchat.api.network.Protocol;
import net.azisaba.interchat.api.network.protocol.GuildSoftDeletePacket;
import net.azisaba.interchat.velocity.VelocityPlugin;
import net.azisaba.interchat.velocity.database.DatabaseManager;
import net.azisaba.interchat.velocity.listener.ChatListener;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.UUID;

public final class VelocityGuildManager extends SQLGuildManager implements GuildManager {
    public VelocityGuildManager() {
        super(DatabaseManager.get());
    }

    public static void markDeleted(@NotNull CommandSource source, long guildId) {
        try {
            DatabaseManager.get().query("UPDATE `guilds` SET `deleted` = 1 WHERE `id` = ?", stmt -> {
                stmt.setLong(1, guildId);
                stmt.executeUpdate();
            });
            DatabaseManager.get().submitLog(guildId, source, "Deleted guild (soft)");
            DatabaseManager.get().query("UPDATE `players` SET `selected_guild` = -1 WHERE `selected_guild` = ?", stmt -> {
                stmt.setLong(1, guildId);
                stmt.executeUpdate();
            });
            DatabaseManager.get().query("UPDATE `players` SET `focused_guild` = -1 WHERE `focused_guild` = ?", stmt -> {
                stmt.setLong(1, guildId);
                stmt.executeUpdate();
            });
            ChatListener.removeCacheWithGuildId(guildId);
            // notify others
            UUID uuid;
            if (source instanceof Player) {
                uuid = ((Player) source).getUniqueId();
            } else {
                uuid = new UUID(0, 0);
            }
            GuildSoftDeletePacket packet = new GuildSoftDeletePacket(guildId, uuid);
            VelocityPlugin.getPlugin().getJedisBox().getPubSubHandler().publish(Protocol.GUILD_SOFT_DELETE.getName(), packet);
        } catch (SQLException e) {
            Logger.getCurrentLogger().error("Failed to mark guild as deleted", e);
        }
    }
}
