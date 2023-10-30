package net.azisaba.interchat.api.text;

import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.data.UserDataProvider;
import net.azisaba.interchat.api.guild.Guild;
import net.azisaba.interchat.api.user.User;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageFormatter {
    private static final Pattern PREFIX_PATTERN = Pattern.compile("%\\{prefix(:[a-zA-Z0-9_.\\-]*(:.+)?)?}");
    private static final Pattern SUFFIX_PATTERN = Pattern.compile("%\\{suffix(:[a-zA-Z0-9_.\\-]*(:.+)?)?}");

    /**
     * @deprecated Use {@link #format(String, Guild, String, User, String, String, String, Map)} instead
     */
    @Deprecated
    public static @NotNull String format(
            @NotNull String format,
            @NotNull Guild guild,
            @NotNull String server,
            @NotNull User sender,
            @NotNull String message,
            @Nullable String transliteratedMessage
    ) {
        return format(format, guild, server, sender, null, message, transliteratedMessage);
    }

    /**
     * @deprecated Use {@link #format(String, Guild, String, User, String, String, String, Map)} instead
     */
    @Deprecated
    public static @NotNull String format(
            @NotNull String format,
            @NotNull Guild guild,
            @NotNull String server,
            @NotNull User sender,
            @Nullable String nickname,
            @NotNull String message,
            @Nullable String transliteratedMessage
    ) {
        return format(format, guild, server, sender, nickname, message, transliteratedMessage, Collections.emptyMap());
    }

    /**
     * Formats the message using arguments.
     * @param format the "format"
     * @param guild the guild
     * @param server the server the message was sent from
     * @param sender the sender
     * @param nickname the nickname of the sender
     * @param message the original message
     * @param transliteratedMessage the transliterated message ({@link Transliterator})
     * @param serverAlias map of current server -&gt; server name used for %prefix and %suffix
     * @return the formatted message
     */
    public static @NotNull String format(
            @NotNull String format,
            @NotNull Guild guild,
            @NotNull String server,
            @NotNull User sender,
            @Nullable String nickname,
            @NotNull String message,
            @Nullable String transliteratedMessage,
            @NotNull Map<@NotNull String, @NotNull String> serverAlias
    ) {
        String msg = message;
        String preReplace = "";
        String preReplaceB = ""; // with bracket
        if (transliteratedMessage != null) {
            msg = transliteratedMessage;
            preReplace = message;
            preReplaceB = "(" + message + ")";
        }
        UserDataProvider userDataProvider = InterChatProvider.get().getUserDataProvider();
        Map<String, String> prefix = userDataProvider.getPrefix(sender.id());
        Map<String, String> suffix = userDataProvider.getSuffix(sender.id());
        AtomicReference<String> atomicFormat = new AtomicReference<>(format);
        BiFunction<Map<String, String>, String, String> getValueOrGlobal = (map, key) -> {
            userDataProvider.requestUpdate(sender.id(), key);
            if (map.containsKey(key)) {
                return map.get(key);
            } else {
                return map.get("global");
            }
        };
        BiConsumer<Matcher, Map<String, String>> consumer = (matcher, map) -> {
            while (matcher.find()) {
                String mServer = matcher.group(1);
                if (mServer != null) mServer = mServer.substring(1);
                if (mServer == null || mServer.isEmpty()) {
                    mServer = serverAlias.getOrDefault(server, server);
                }
                String mDefault = matcher.group(2);
                if (mDefault != null) {
                    mDefault = mDefault.substring(1);
                } else {
                    mDefault = "";
                }
                atomicFormat.set(atomicFormat.get().replace(matcher.group(), getOrDefault(getValueOrGlobal.apply(map, mServer), mDefault)));
            }
        };
        consumer.accept(PREFIX_PATTERN.matcher(format), prefix);
        consumer.accept(SUFFIX_PATTERN.matcher(format), suffix);
        return atomicFormat.get()
                .replace("%gname", guild.name())
                .replace("%server", server)
                .replace("%playername", sender.name())
                .replace("%username-n", Optional.ofNullable(nickname).orElse(sender.name()))
                .replace("%username", Optional.ofNullable(nickname).map(s -> "~" + s).orElse(sender.name()))
                .replace("%msg", msg)
                .replace("%prereplace-b", preReplaceB)
                .replace("%prereplace", preReplace)
                .replace("%prefix", getOrDefault(getValueOrGlobal.apply(prefix, serverAlias.getOrDefault(server, server)), ""))
                .replace("%suffix", getOrDefault(getValueOrGlobal.apply(suffix, serverAlias.getOrDefault(server, server)), ""))
                ;
    }

    @Contract(value = "null, _ -> param2; !null, _ -> param1", pure = true)
    private static String getOrDefault(String s, String def) {
        return s == null ? def : s;
    }
}
