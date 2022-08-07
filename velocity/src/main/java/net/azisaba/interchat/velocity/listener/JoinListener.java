package net.azisaba.interchat.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.Logger;
import net.azisaba.interchat.velocity.database.DatabaseManager;

import java.sql.SQLException;

public final class JoinListener {
    @Subscribe
    public void onLogin(LoginEvent e) {
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                DatabaseManager.get().runPrepareStatement("INSERT INTO `players` (`id`, `name`, `accepting_invites`) VALUES (?, ?, 0) ON DUPLICATE KEY UPDATE `name` = VALUES(`name`)", stmt -> {
                    stmt.setString(1, e.getPlayer().getUniqueId().toString());
                    stmt.setString(2, e.getPlayer().getUsername());
                    stmt.execute();
                });
                Logger.getCurrentLogger().info("Registered player " + e.getPlayer().getUsername() + " with id " + e.getPlayer().getUniqueId());
            } catch (SQLException ex) {
                Logger.getCurrentLogger().error("Failed to register player " + e.getPlayer().getUsername() + " with id " + e.getPlayer().getUniqueId(), ex);
            }
        });
    }
}
