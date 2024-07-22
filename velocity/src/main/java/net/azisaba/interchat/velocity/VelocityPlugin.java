package net.azisaba.interchat.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.azisaba.interchat.api.InterChatProviderProvider;
import net.azisaba.interchat.api.network.JedisBox;
import net.azisaba.interchat.api.network.ProxyPacketListener;
import net.azisaba.interchat.api.network.Side;
import net.azisaba.interchat.api.text.Messages;
import net.azisaba.interchat.api.util.MapEx;
import net.azisaba.interchat.velocity.command.*;
import net.azisaba.interchat.velocity.database.DatabaseConfig;
import net.azisaba.interchat.velocity.database.DatabaseManager;
import net.azisaba.interchat.velocity.listener.ChatListener;
import net.azisaba.interchat.velocity.listener.JoinListener;
import net.azisaba.interchat.velocity.network.ProxyPacketListenerImpl;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Plugin(id = "interchat", name = "InterChat", version = "1.0.0-SNAPSHOT", authors = "Azisaba Network",
        url = "https://github.com/AzisabaNetwork/InterChat", description = "Adds guild-like features to Velocity servers",
        dependencies = {@Dependency(id = "luckperms", optional = true)})
public class VelocityPlugin {
    private static VelocityPlugin plugin;
    private final ProxyPacketListener packetListener = new ProxyPacketListenerImpl(this);
    private final ProxyServer server;
    private final Logger logger;
    private final VelocityInterChat api;
    private final JedisBox jedisBox;
    private final DatabaseManager databaseManager;
    private final DatabaseConfig databaseConfig;
    private final Map<String, String> serverAlias = new HashMap<>();
    private final boolean noWorkers;

    @Inject
    public VelocityPlugin(@NotNull ProxyServer server, @NotNull Logger logger, @DataDirectory @NotNull Path dataDirectory) throws IOException, SQLException {
        plugin = this;
        this.server = server;
        this.logger = logger;
        logger.info("Loading translations...");
        Messages.load();
        logger.info("Loading config...");
        MapEx<Object, Object> config = new MapEx<>(new Yaml().load(Files.newInputStream(dataDirectory.resolve("config.yml"))));
        this.jedisBox = createJedisBos(config);
        MapEx<Object, Object> databaseConfig = config.getMap("database");
        if (databaseConfig == null) {
            throw new RuntimeException("database section is not found in config.yml");
        }
        if (config.getMap("server-alias") != null) {
            Objects.requireNonNull(config.getMap("server-alias")).forEach((k, v) -> serverAlias.put(String.valueOf(k), String.valueOf(v)));
        }
        this.noWorkers = config.getBoolean("no-workers", false);

        logger.info("Connecting to database...");
        this.databaseConfig = new DatabaseConfig(databaseConfig);
        this.databaseManager = new DatabaseManager(this.databaseConfig.createDataSource());
        InterChatProviderProvider.register(this.api = new VelocityInterChat(this));
    }

    @Contract("_ -> new")
    private @NotNull JedisBox createJedisBos(@NotNull MapEx<Object, Object> config) {
        MapEx<Object, Object> redisMap = config.getMap("redis");
        if (redisMap == null) {
            throw new RuntimeException("redis section is not found in config.yml");
        }
        String redisHostname = redisMap.getString("hostname", "localhost");
        int redisPort = redisMap.getInt("port", 6379);
        String redisUsername = redisMap.getString("username");
        String redisPassword = redisMap.getString("password");
        return new JedisBox(
                Side.PROXY,
                net.azisaba.interchat.api.Logger.createByProxy(logger),
                packetListener,
                redisHostname,
                redisPort,
                redisUsername,
                redisPassword);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e) {
        server.getEventManager().register(this, new ChatListener());
        server.getEventManager().register(this, new JoinListener());
        server.getCommandManager().register(new GuildCommand(this).createCommand());
        server.getCommandManager().register(new GuildAdminCommand().createCommand());
        server.getCommandManager().register(new GShortCommand(this).createCommand());
        server.getCommandManager().register(new GSShortCommand(this).createCommand());
        server.getCommandManager().register(new GuildTestCommand().createCommand());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent e) {
        jedisBox.close();
        databaseManager.close();
    }

    @Contract(pure = true)
    @NotNull
    public final ProxyServer getServer() {
        return server;
    }

    @Contract(pure = true)
    @NotNull
    public final Logger getLogger() {
        return logger;
    }

    @Contract(pure = true)
    @NotNull
    public final VelocityInterChat getAPI() {
        return api;
    }

    @Contract(pure = true)
    @NotNull
    public final JedisBox getJedisBox() {
        return jedisBox;
    }

    @Contract(pure = true)
    @NotNull
    public final DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    @Contract(pure = true)
    @NotNull
    public final DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

    @Contract(pure = true)
    @NotNull
    public static VelocityPlugin getPlugin() {
        return plugin;
    }

    @Contract(pure = true)
    @NotNull
    public static ProxyServer getProxyServer() {
        return getPlugin().getServer();
    }

    public @NotNull Map<String, String> getServerAlias() {
        return serverAlias;
    }

    public boolean isNoWorkers() {
        return noWorkers;
    }
}
