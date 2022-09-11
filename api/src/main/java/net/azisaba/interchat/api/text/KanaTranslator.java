package net.azisaba.interchat.api.text;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;

public class KanaTranslator {
    public static final char SKIP_CHAR = '#';
    @NotNull
    public static final String SKIP_CHAR_STRING = Character.toString(SKIP_CHAR);
    private static final String REGEX_URL = "https?://[\\w/:%#$&?()~.=+\\-]+";

    @Blocking
    public static @Nullable String translateSync(@NotNull String text) {
        if (text.startsWith(SKIP_CHAR_STRING)) {
            return null;
        }
        if (text.getBytes(StandardCharsets.UTF_8).length > text.length() || text.matches("[\\uff61-\\uff9f]+")) {
            return null;
        }
        text = text.replaceAll(REGEX_URL, " ");
        if (text.trim().length() == 0) {
            return null;
        }
        text = RomajiTextReader.parse(text);
        text = text.replaceAll("&([0-9a-fklmnor])", "\u00a7\\\\$1");
        text = Transliterator.transliterate(text);
        text = text.replace("\u00a7\uffe5", "\u00a7");
        text = text.replace("\u00a7\uff10", "\u00a70");
        text = text.replace("\u00a7\uff11", "\u00a71");
        text = text.replace("\u00a7\uff12", "\u00a72");
        text = text.replace("\u00a7\uff13", "\u00a73");
        text = text.replace("\u00a7\uff14", "\u00a74");
        text = text.replace("\u00a7\uff15", "\u00a75");
        text = text.replace("\u00a7\uff16", "\u00a76");
        text = text.replace("\u00a7\uff17", "\u00a77");
        text = text.replace("\u00a7\uff18", "\u00a78");
        text = text.replace("\u00a7\uff19", "\u00a79");
        text = text.replace("\u00a7\u3042", "\u00a7a");
        text = text.replace("\u00a7\u3048", "\u00a7e");
        return text.trim();
    }
}
