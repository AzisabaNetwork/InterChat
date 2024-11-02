package net.azisaba.interchat.velocity.command.argument;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.user.User;
import net.azisaba.interchat.velocity.VelocityPlugin;
import net.azisaba.interchat.velocity.text.VMessages;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

public class UUIDArgumentType {
    private static final Gson GSON = new Gson();
    private static final SimpleCommandExceptionType STATIC_INVALID_UUID = new SimpleCommandExceptionType(new LiteralMessage("Invalid UUID"));
    private static final BiFunction<CommandContext<CommandSource>, Object, CommandSyntaxException> INVALID_UUID = (ctx, o) -> {
        String formatted = VMessages.format(ctx.getSource(), "command.error.invalid_uuid", o);
        return new CommandSyntaxException(STATIC_INVALID_UUID, new LiteralMessage(formatted));
    };

    @Contract(pure = true)
    public static @NotNull StringArgumentType uuid() {
        return StringArgumentType.string();
    }

    public static @NotNull UUID get(@NotNull CommandContext<CommandSource> ctx, @NotNull String name) throws CommandSyntaxException {
        String value = StringArgumentType.getString(ctx, name);
        Optional<Player> opt = VelocityPlugin.getProxyServer().getPlayer(value);
        if (opt.isPresent()) {
            return opt.get().getUniqueId();
        }
        try {
            UUID uuid = UUID.fromString(value);
            if (uuid.version() == 4) {
                return uuid;
            } else {
                throw INVALID_UUID.apply(ctx, value);
            }
        } catch (IllegalArgumentException e) {
            throw INVALID_UUID.apply(ctx, value);
        }
    }

    /**
     * Attempts to fetch the UUID of the player with the given name from these sources:
     * <ol>
     *     <li>Current Velocity instance</li>
     *     <li>UUID itself, if parsable</li>
     *     <li>Database</li>
     *     <li>Mojang API</li>
     * </ol>
     * @param ctx context
     * @param name name of the argument
     * @return UUID of the player
     * @throws CommandSyntaxException if the player is not found
     */
    @Blocking
    public static @NotNull UUID getPlayerWithAPI(@NotNull CommandContext<CommandSource> ctx, @NotNull String name) throws CommandSyntaxException {
        try {
            return get(ctx, name);
        } catch (CommandSyntaxException ignored) {}
        String value = StringArgumentType.getString(ctx, name);
        List<User> users = InterChatProvider.get().getUserManager().fetchUserByUsername(value).join();
        if (users.size() == 1) {
            return users.get(0).id();
        }
        if (users.isEmpty()) {
            throw INVALID_UUID.apply(ctx, value);
        }
        try {
            // get from Mojang API as last resort
            return fetchUUIDFromMojangAPI(value);
        } catch (IOException e) {
            throw INVALID_UUID.apply(ctx, value);
        }
    }

    private static @NotNull UUID fetchUUIDFromMojangAPI(@NotNull String name) throws IOException {
        URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.addRequestProperty("Accept", "application/json");
        conn.addRequestProperty("User-Agent", "InterChat/hard-coded-version (https://github.com/AzisabaNetwork/InterChat)");
        conn.setRequestMethod("GET");
        conn.connect();
        StringBuilder sb = new StringBuilder();
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("HTTP response code: " + responseCode);
        }
        String line;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        conn.disconnect();
        return parseUUID(GSON.fromJson(sb.toString(), JsonObject.class).get("id").getAsString());
    }

    @Contract("_ -> new")
    private static @NotNull UUID parseUUID(@NotNull String uuid) {
        return UUID.fromString(uuid.replaceAll("(?i)([\\da-f]{8})([\\da-f]{4})([\\da-f]{4})([\\da-f]{4})([\\da-f]{12})", "$1-$2-$3-$4-$5"));
    }
}
