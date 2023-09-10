package net.azisaba.interchat.velocity.database;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.zaxxer.hikari.HikariDataSource;
import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.Logger;
import net.azisaba.interchat.api.util.QueryExecutor;
import net.azisaba.interchat.velocity.VelocityPlugin;
import net.azisaba.interchat.api.util.SQLThrowableConsumer;
import net.azisaba.interchat.api.util.SQLThrowableFunction;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public final class DatabaseManager implements QueryExecutor {
    private final @NotNull HikariDataSource dataSource;

    public DatabaseManager(@NotNull HikariDataSource dataSource) throws SQLException {
        this.dataSource = dataSource;
        createTables();
    }

    private void createTables() throws SQLException {
        useStatement(statement -> {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `guilds` (" +
                    "  `id` BIGINT NOT NULL AUTO_INCREMENT," +
                    "  `name` VARCHAR(255) NOT NULL UNIQUE," +
                    "  `format` VARCHAR(255) NOT NULL," +
                    "  `capacity` INT NOT NULL DEFAULT 100," +
                    "  `deleted` TINYINT(1) NOT NULL DEFAULT 0," +
                    "  `open` TINYINT(1) NOT NULL DEFAULT 0," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `guild_members` (" +
                    "  `guild_id` BIGINT NOT NULL," +
                    "  `uuid` VARCHAR(36) NOT NULL," +
                    "  `role` VARCHAR(64) NOT NULL DEFAULT 'MEMBER'," +
                    "  `nickname` VARCHAR(32) DEFAULT NULL," +
                    "  PRIMARY KEY (`guild_id`, `uuid`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `guild_invites` (" +
                    "  `guild_id` BIGINT NOT NULL," +
                    "  `target` VARCHAR(36) NOT NULL," +
                    "  `actor` VARCHAR(36) NOT NULL," +
                    "  `expires_at` BIGINT NOT NULL," +
                    "  PRIMARY KEY (`guild_id`, `target`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `guild_logs` (" +
                    "  `id` BIGINT NOT NULL AUTO_INCREMENT," +
                    "  `guild_id` BIGINT NOT NULL," +
                    "  `actor` VARCHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000'," +
                    "  `actor_name` VARCHAR(36) NOT NULL," +
                    "  `time` BIGINT NOT NULL," +
                    "  `description` TEXT NOT NULL," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `guild_bans` (" +
                    "  `guild_id` BIGINT NOT NULL," +
                    "  `uuid` VARCHAR(36) NOT NULL," +
                    "  `reason` VARCHAR(255)," +
                    "  `reason_public` TINYINT(1) NOT NULL," +
                    "  UNIQUE KEY (guild_id, uuid)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `players` (" +
                    "  `id` VARCHAR(36) NOT NULL," +
                    "  `name` VARCHAR(36) NOT NULL," +
                    "  `selected_guild` BIGINT NOT NULL DEFAULT -1," +
                    "  `focused_guild` BIGINT NOT NULL DEFAULT -1," + // able to chat to guild without command
                    "  `accepting_invites` TINYINT(1) NOT NULL DEFAULT 1," +
                    "  `translate_kana` TINYINT(1) NOT NULL DEFAULT 1," +
                    "  `hide_all_until` BIGINT NOT NULL DEFAULT 0," +
                    "  PRIMARY KEY (`id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
        });
    }

    @NotNull
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Contract(pure = true)
    public <R> R use(@NotNull SQLThrowableFunction<Connection, R> action) throws SQLException {
        try (Connection connection = getConnection()) {
            return action.apply(connection);
        }
    }

    @Contract(pure = true)
    public void use(@NotNull SQLThrowableConsumer<Connection> action) throws SQLException {
        try (Connection connection = getConnection()) {
            action.accept(connection);
        }
    }

    @Contract(pure = true)
    @Override
    public void query(@Language("SQL") @NotNull String sql, @NotNull SQLThrowableConsumer<PreparedStatement> action) throws SQLException {
        use(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                action.accept(statement);
            }
        });
    }

    @Contract(pure = true)
    @Override
    public void queryWithGeneratedKeys(@NotNull String sql, @NotNull SQLThrowableConsumer<PreparedStatement> action) throws SQLException {
        use(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                action.accept(statement);
            }
        });
    }

    @Contract(pure = true)
    public <R> R getPrepareStatement(@Language("SQL") @NotNull String sql, @NotNull SQLThrowableFunction<PreparedStatement, R> action) throws SQLException {
        return use(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                return action.apply(statement);
            }
        });
    }

    @Contract(pure = true)
    public void useStatement(@NotNull SQLThrowableConsumer<Statement> action) throws SQLException {
        use(connection -> {
            try (Statement statement = connection.createStatement()) {
                action.accept(statement);
            }
        });
    }

    /**
     * Closes the data source.
     */
    public void close() {
        dataSource.close();
    }

    public void submitLog(long guildId, @Nullable String actor, @NotNull String actorName, @NotNull String description) {
        InterChatProvider.get().getAsyncExecutor().execute(() -> {
            try {
                query("INSERT INTO `guild_logs` (`guild_id`, `actor`, `actor_name`, `time`, `description`) VALUES (?, ?, ?, ?, ?)", statement -> {
                    statement.setLong(1, guildId);
                    statement.setString(2, actor);
                    statement.setString(3, actorName);
                    statement.setLong(4, System.currentTimeMillis());
                    statement.setString(5, description);
                    statement.executeUpdate();
                });
            } catch (SQLException e) {
                Logger.getCurrentLogger().error("Failed to submit log", e);
            }
        });
    }

    public void submitLog(long guildId, @NotNull Player player, @NotNull String description) {
        submitLog(guildId, player.getUniqueId().toString(), player.getUsername(), description);
    }

    public void submitLog(long guildId, @NotNull CommandSource source, @NotNull String description) {
        if (source instanceof Player player) {
            submitLog(guildId, player, description);
        } else {
            submitLog(guildId, new UUID(0, 0).toString(), "Console", description);
        }
    }

    @NotNull
    public static DatabaseManager get() {
        return VelocityPlugin.getPlugin().getDatabaseManager();
    }
}
