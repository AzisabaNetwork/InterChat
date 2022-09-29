package net.azisaba.interchat.api;

import net.azisaba.interchat.api.guild.GuildManager;
import net.azisaba.interchat.api.user.UserManager;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

public interface InterChat {
    @NotNull
    Logger getLogger();

    @NotNull
    GuildManager getGuildManager();

    @NotNull
    UserManager getUserManager();

    @NotNull
    Executor getAsyncExecutor();
}
