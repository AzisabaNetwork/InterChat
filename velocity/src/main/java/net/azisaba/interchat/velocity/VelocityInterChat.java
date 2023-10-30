package net.azisaba.interchat.velocity;

import net.azisaba.interchat.api.data.*;
import net.azisaba.interchat.api.guild.GuildManager;
import net.azisaba.interchat.api.InterChat;
import net.azisaba.interchat.api.Logger;
import net.azisaba.interchat.api.user.UserManager;
import net.azisaba.interchat.velocity.database.DatabaseManager;
import net.azisaba.interchat.velocity.guild.VelocityGuildManager;
import net.azisaba.interchat.api.user.SQLUserManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

public final class VelocityInterChat implements InterChat {
    private final Logger logger;
    private final GuildManager guildManager = new VelocityGuildManager();
    private final UserManager userManager = new SQLUserManager(DatabaseManager.get());
    private final Executor asyncExecutor;
    private final UserDataProvider userDataProvider;

    public VelocityInterChat(@NotNull VelocityPlugin plugin) {
        this.logger = Logger.createByProxy(plugin.getLogger());
        this.asyncExecutor = r -> plugin.getServer().getScheduler().buildTask(plugin, r).schedule();
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
