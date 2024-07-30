package net.azisaba.interchat.spigot;

import net.azisaba.interchat.api.InterChat;
import net.azisaba.interchat.api.Logger;
import net.azisaba.interchat.api.data.CompoundUserDataProvider;
import net.azisaba.interchat.api.data.DummyUserDataProvider;
import net.azisaba.interchat.api.data.LuckPermsUserDataProvider;
import net.azisaba.interchat.api.data.UserDataProvider;
import net.azisaba.interchat.api.data.WorkersUserDataProvider;
import net.azisaba.interchat.api.guild.GuildManager;
import net.azisaba.interchat.api.guild.SQLGuildManager;
import net.azisaba.interchat.api.user.SQLUserManager;
import net.azisaba.interchat.api.user.UserManager;
import net.azisaba.interchat.spigot.database.DatabaseManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

public class SpigotInterChat implements InterChat {
    private final Logger logger;
    private final GuildManager guildManager = new SQLGuildManager(DatabaseManager.get());
    private final UserManager userManager = new SQLUserManager(DatabaseManager.get());
    private final Executor asyncExecutor;
    private final UserDataProvider userDataProvider;

    public SpigotInterChat(@NotNull SpigotPlugin plugin) {
        this.logger = Logger.createFromJavaLogger(plugin.getLogger());
        this.asyncExecutor = r -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, r);
        if (!plugin.isNoWorkers() && LuckPermsUserDataProvider.isAvailable()) {
            userDataProvider = new CompoundUserDataProvider(new WorkersUserDataProvider(), new LuckPermsUserDataProvider());
        } else if (LuckPermsUserDataProvider.isAvailable()) {
            userDataProvider = new LuckPermsUserDataProvider();
        } else {
            userDataProvider = DummyUserDataProvider.INSTANCE;
        }
    }

    @Contract(pure = true)
    @Override
    public @NotNull Logger getLogger() {
        return logger;
    }

    @Contract(pure = true)
    @Override
    public @NotNull GuildManager getGuildManager() {
        return guildManager;
    }

    @Contract(pure = true)
    @Override
    public @NotNull UserManager getUserManager() {
        return userManager;
    }

    @Contract(pure = true)
    @Override
    public @NotNull Executor getAsyncExecutor() {
        return asyncExecutor;
    }

    @Override
    public @NotNull UserDataProvider getUserDataProvider() {
        return userDataProvider;
    }
}
