package net.azisaba.interchat.api.text;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class KanaTranslator {
    public static final char SKIP_CHAR = '#';
    @NotNull
    public static final String SKIP_CHAR_STRING = Character.toString(SKIP_CHAR);
    private static final String REGEX_URL = "https?://[\\w/:%#$&?()~.=+\\-]+";
    private static final Map<String, List<String>> CACHE = new ConcurrentHashMap<>();

    @Blocking
    public static @NotNull List<@NotNull String> translateSync(@NotNull String text) {
        return CACHE.computeIfAbsent(text, KanaTranslator::translateSync0);
    }

    @Blocking
    private static @NotNull List<@NotNull String> translateSync0(@NotNull String text) {
        if (text.startsWith(SKIP_CHAR_STRING)) {
            return Collections.emptyList();
        }
        if (text.getBytes(StandardCharsets.UTF_8).length > text.length() || text.matches("[\\uff61-\\uff9f]+")) {
            return Collections.emptyList();
        }
        text = text.replaceAll(REGEX_URL, " ");
        if (text.trim().length() == 0) {
            return Collections.emptyList();
        }
        text = RomajiTextReader.parse(text);
        text = text.replaceAll("&([0-9a-fklmnor])", "\u00a7\\\\$1");
        return Transliterator.transliterate(text)
                .stream()
                .map(s -> {
                    s = s.replace("\u00a7\uffe5", "\u00a7");
                    s = s.replace("\u00a7\uff10", "\u00a70");
                    s = s.replace("\u00a7\uff11", "\u00a71");
                    s = s.replace("\u00a7\uff12", "\u00a72");
                    s = s.replace("\u00a7\uff13", "\u00a73");
                    s = s.replace("\u00a7\uff14", "\u00a74");
                    s = s.replace("\u00a7\uff15", "\u00a75");
                    s = s.replace("\u00a7\uff16", "\u00a76");
                    s = s.replace("\u00a7\uff17", "\u00a77");
                    s = s.replace("\u00a7\uff18", "\u00a78");
                    s = s.replace("\u00a7\uff19", "\u00a79");
                    s = s.replace("\u00a7\u3042", "\u00a7a");
                    s = s.replace("\u00a7\u3048", "\u00a7e");
                    return s.trim();
                })
                .collect(Collectors.toList());
    }
}
