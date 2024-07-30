package net.azisaba.interchat.spigot.guild;

import net.azisaba.interchat.api.guild.SQLGuildManager;
import net.azisaba.interchat.spigot.database.DatabaseManager;

public class SpigotGuildManager extends SQLGuildManager {
    public SpigotGuildManager() {
        super(DatabaseManager.get());
    }
}
