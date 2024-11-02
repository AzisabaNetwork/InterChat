package net.azisaba.interchat.api.text;

import net.azisaba.interchat.api.InterChatProvider;
import net.azisaba.interchat.api.WorldPos;
import net.azisaba.interchat.api.data.SenderInfo;
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
     * @deprecated Use {@link #format(String, Guild, SenderInfo, String, String, Map)} instead
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
     * @deprecated Use {@link #format(String, Guild, SenderInfo, String, String, Map)} instead
     */
    @Deprecated
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
        return format(format, guild, new SenderInfo(sender, server, nickname, null), message, transliteratedMessage, serverAlias);
    }


    /**
     * Formats the message using arguments.
     * @param format the "format"
     * @param guild the guild
     * @param senderInfo the sender data
     * @param message the original message
     * @param transliteratedMessage the transliterated message ({@link Transliterator})
     * @param serverAlias map of current server -&gt; server name used for %prefix and %suffix
     * @return the formatted message
     */
    public static @NotNull String format(
            @NotNull String format,
            @NotNull Guild guild,
            @NotNull SenderInfo senderInfo,
            @NotNull String message,
            @Nullable String transliteratedMessage,
            @NotNull Map<@NotNull String, @NotNull String> serverAlias
    ) {
        String msg = transliteratedMessage != null ? transliteratedMessage : message;
        String preReplace = transliteratedMessage != null ? message : "";
        String preReplaceB = transliteratedMessage != null ? "(" + message + ")" : ""; // with bracket
        UserDataProvider userDataProvider = InterChatProvider.get().getUserDataProvider();
        Map<String, String> prefix = userDataProvider.getPrefix(senderInfo.getUser().id());
        Map<String, String> suffix = userDataProvider.getSuffix(senderInfo.getUser().id());
        AtomicReference<String> atomicFormat = new AtomicReference<>(format);
        BiFunction<Map<String, String>, String, String> getValueOrGlobal = (map, key) -> {
            userDataProvider.requestUpdate(senderInfo.getUser().id(), key);
            if (map.containsKey(key)) {
                return map.get(key);
            } else {
                return map.get("global");
            }
        };
        extractPattern(format, senderInfo, serverAlias, prefix, suffix, atomicFormat, getValueOrGlobal);
        return atomicFormat.get()
                .replace("%gname", guild.name())
                .replace("%server", senderInfo.getServer())
                .replace("%playername", senderInfo.getUser().name())
                .replace("%username-n", Optional.ofNullable(senderInfo.getNickname()).orElse(senderInfo.getUser().name()))
                .replace("%username", Optional.ofNullable(senderInfo.getNickname()).map(s -> "~" + s).orElse(senderInfo.getUser().name()))
                .replace("%msg", msg)
                .replace("%prereplace-b", preReplaceB)
                .replace("%prereplace", preReplace)
                .replace("%prefix", getOrDefault(getValueOrGlobal.apply(prefix, serverAlias.getOrDefault(senderInfo.getServer(), senderInfo.getServer())), ""))
                .replace("%suffix", getOrDefault(getValueOrGlobal.apply(suffix, serverAlias.getOrDefault(senderInfo.getServer(), senderInfo.getServer())), ""))
                .replace("%world", Optional.ofNullable(senderInfo.getPos()).map(WorldPos::getWorld).orElse("null"))
                .replace("%x", Optional.ofNullable(senderInfo.getPos()).map(WorldPos::getX).orElse(0).toString())
                .replace("%y", Optional.ofNullable(senderInfo.getPos()).map(WorldPos::getY).orElse(0).toString())
                .replace("%z", Optional.ofNullable(senderInfo.getPos()).map(WorldPos::getZ).orElse(0).toString())
                ;
    }

    public static @NotNull String formatPrivateChat(
            @NotNull String format,
            @NotNull SenderInfo sender,
            @NotNull User receiver,
            @NotNull String message,
            @Nullable String transliteratedMessage,
            @NotNull Map<@NotNull String, @NotNull String> serverAlias
    ) {
        String msg = transliteratedMessage != null ? transliteratedMessage : message;
        String preReplace = transliteratedMessage != null ? message : "";
        String preReplaceB = transliteratedMessage != null ? "(" + message + ")" : ""; // with bracket
        UserDataProvider userDataProvider = InterChatProvider.get().getUserDataProvider();
        Map<String, String> senderPrefix = userDataProvider.getPrefix(sender.getUser().id());
        Map<String, String> senderSuffix = userDataProvider.getSuffix(sender.getUser().id());
        AtomicReference<String> atomicFormat = new AtomicReference<>(format);
        BiFunction<Map<String, String>, String, String> getValueOrGlobal = (map, key) -> {
            if (map.containsKey(key)) {
                return map.get(key);
            } else {
                return map.get("global");
            }
        };
        extractPattern(format, sender, serverAlias, senderPrefix, senderSuffix, atomicFormat, getValueOrGlobal);
        return atomicFormat.get()
                .replace("%s-server", sender.getServer())
                .replace("%s-playername", sender.getUser().name())
                .replace("%s-prefix", getOrDefault(getValueOrGlobal.apply(senderPrefix, serverAlias.getOrDefault(sender.getServer(), sender.getServer())), ""))
                .replace("%s-suffix", getOrDefault(getValueOrGlobal.apply(senderSuffix, serverAlias.getOrDefault(sender.getServer(), sender.getServer())), ""))
                .replace("%s-world", Optional.ofNullable(sender.getPos()).map(WorldPos::getWorld).orElse("null"))
                .replace("%s-x", Optional.ofNullable(sender.getPos()).map(WorldPos::getX).orElse(0).toString())
                .replace("%s-y", Optional.ofNullable(sender.getPos()).map(WorldPos::getY).orElse(0).toString())
                .replace("%s-z", Optional.ofNullable(sender.getPos()).map(WorldPos::getZ).orElse(0).toString())
                .replace("%r-playername", receiver.name())
                .replace("%msg", msg)
                .replace("%prereplace-b", preReplaceB)
                .replace("%prereplace", preReplace)
                ;
    }

    private static void extractPattern(@NotNull String format, @NotNull SenderInfo sender, @NotNull Map<@NotNull String, @NotNull String> serverAlias, Map<String, String> senderPrefix, Map<String, String> senderSuffix, AtomicReference<String> atomicFormat, BiFunction<Map<String, String>, String, String> getValueOrGlobal) {
        BiConsumer<Matcher, Map<String, String>> consumer = (matcher, map) -> {
            while (matcher.find()) {
                String mServer = matcher.group(1);
                if (mServer != null) mServer = mServer.substring(1);
                if (mServer == null || mServer.isEmpty()) {
                    mServer = serverAlias.getOrDefault(sender.getServer(), sender.getServer());
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
        consumer.accept(PREFIX_PATTERN.matcher(format), senderPrefix);
        consumer.accept(SUFFIX_PATTERN.matcher(format), senderSuffix);
    }

    @Contract(value = "null, _ -> param2; !null, _ -> param1", pure = true)
    private static String getOrDefault(String s, String def) {
        return s == null ? def : s;
    }
}
