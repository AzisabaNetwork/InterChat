package net.azisaba.interchat.spigot;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.azisaba.interchat.api.InterChatProviderProvider;
import net.azisaba.interchat.api.network.BackendPacketListener;
import net.azisaba.interchat.api.network.JedisBox;
import net.azisaba.interchat.api.network.Side;
import net.azisaba.interchat.api.text.Messages;
import net.azisaba.interchat.api.util.MapEx;
import net.azisaba.interchat.spigot.database.DatabaseConfig;
import net.azisaba.interchat.spigot.database.DatabaseManager;
import net.azisaba.interchat.spigot.listener.PlayerListener;
import net.azisaba.interchat.spigot.network.BackendPacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpigotPlugin extends JavaPlugin {
    private final BackendPacketListener packetListener = new BackendPacketListenerImpl(this);
    private JedisBox jedisBox;
    private boolean noWorkers;
    private DatabaseConfig databaseConfig;
    private DatabaseManager databaseManager;
    private SpigotInterChat api;
    public String server = "undefined";

    @Override
    public void onEnable() {
        getLogger().info("Loading translations...");
        try {
            Messages.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load translations", e);
        }
        getLogger().info("Loading config...");
        saveDefaultConfig();
        MapEx<Object, Object> config;
        try {
            config = new MapEx<>(new Yaml().load(new FileInputStream(new File(getDataFolder(), "config.yml"))));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        this.jedisBox = createJedisBox(config);
        MapEx<Object, Object> databaseConfig = config.getMap("database");
        if (databaseConfig == null) {
            throw new RuntimeException("database section is not found in config.yml");
        }
        this.noWorkers = config.getBoolean("no-workers", false);

        getLogger().info("Connecting to database...");
        this.databaseConfig = new DatabaseConfig(databaseConfig);
        try {
            this.databaseManager = new DatabaseManager(this.databaseConfig.createDataSource());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to setup database", e);
        }
        InterChatProviderProvider.register(this.api = new SpigotInterChat(this));

        PluginMessageListener listener = (channel, player, message) -> {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subChannel = in.readUTF();
            if (subChannel.equals("GetServer")) {
                String newServer = in.readUTF();
                if (!newServer.equals(server)) {
                    getLogger().info("Server name is " + newServer);
                }
                server = newServer;
            }
        };
        try {
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", listener);
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "InterChat");
            getLogger().info("Registered legacy plugin messaging channel");
        } catch (Exception ignored) {
        }
        try {
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "bungeecord:main");
            Bukkit.getMessenger().registerIncomingPluginChannel(this, "bungeecord:main", listener);
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "interchat:main");
            getLogger().info("Registered modern plugin messaging channel");
        } catch (Exception ignored) {
        }

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (!players.isEmpty()) {
            fetchServer(players.get(0));
        }
        for (Player player : players) {
            sendSignal(player);
        }
    }

    @Override
    public void onDisable() {
        if (jedisBox != null) {
            jedisBox.close();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    @Contract("_ -> new")
    private @NotNull JedisBox createJedisBox(@NotNull MapEx<Object, Object> config) {
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
                net.azisaba.interchat.api.Logger.createFromJavaLogger(getLogger()),
                packetListener,
                redisHostname,
                redisPort,
                redisUsername,
                redisPassword);
    }

    public @NotNull JedisBox getJedisBox() {
        return Objects.requireNonNull(jedisBox, "JedisBox is not initialized");
    }

    public boolean isNoWorkers() {
        return noWorkers;
    }

    public @NotNull DatabaseManager getDatabaseManager() {
        return Objects.requireNonNull(databaseManager, "DatabaseManager is not initialized");
    }

    public @NotNull SpigotInterChat getApi() {
        return Objects.requireNonNull(api, "SpigotInterChat is not initialized");
    }

    public @NotNull DatabaseConfig getDatabaseConfig() {
        return Objects.requireNonNull(databaseConfig, "DatabaseConfig is not initialized");
    }

    public void fetchServer(@NotNull Player player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServer");
        try {
            player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
        } catch (Exception e) {
            try {
                player.sendPluginMessage(this, "bungeecord:main", out.toByteArray());
            } catch (Exception e2) {
                e2.addSuppressed(e);
                throw e2;
            }
        }
    }

    public void sendSignal(@NotNull Player player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Hello");
        byte[] array = out.toByteArray();
        try {
            player.sendPluginMessage(this, "InterChat", array);
            getLogger().info("Sent InterChat signal for " + player.getName());
        } catch (Exception ignored) {
        }
        try {
            player.sendPluginMessage(this, "interchat:main", array);
            getLogger().info("Sent interchat:main signal for " + player.getName());
        } catch (Exception ignored) {
        }
    }

    public static @NotNull SpigotPlugin getInstance() {
        return getPlugin(SpigotPlugin.class);
    }
}
