package net.azisaba.interchat.api.text;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RomajiTextReader {
    private static final List<Map.Entry<String, String>> LIST = new ArrayList<>();

    private static void add(String key, String value) {
        LIST.add(new AbstractMap.SimpleImmutableEntry<>(key, value));
    }

    public static @NotNull String parse(@NotNull String text) {
        return parse(text, true);
    }

    public static @NotNull String parse(@NotNull String text, boolean colorCodeAware) {
        StringReader reader = new StringReader(text);
        StringBuilder builder = new StringBuilder();
        boolean skip = false;
        do {
            if (colorCodeAware && reader.peek() == '&') {
                skip = !skip;
                builder.append(reader.read(1));
                continue;
            }
            if (skip) {
                builder.append(reader.read(1));
                skip = false;
                continue;
            }
            boolean found = false;
            for (Map.Entry<String, String> data : LIST) {
                if (!found && reader.startsWith(data.getKey())) {
                    builder.append(data.getValue());
                    reader.skip(data.getKey().length());
                    found = true;
                }
            }
            if (!found) {
                builder.append(reader.read(1));
            }
        } while (!reader.isEOF());
        if (skip) {
            builder.append('&');
        }
        return builder.toString();
    }

    static {
        add("a", "あ");
        add("i", "い");
        add("u", "う");
        add("e", "え");
        add("o", "お");
        add("ka", "か");
        add("ki", "き");
        add("ku", "く");
        add("ke", "け");
        add("ko", "こ");
        add("sa", "さ");
        add("si", "し");
        add("su", "す");
        add("se", "せ");
        add("so", "そ");
        add("ta", "た");
        add("ti", "ち");
        add("tu", "つ");
        add("te", "て");
        add("to", "と");
        add("na", "な");
        add("ni", "に");
        add("nu", "ぬ");
        add("ne", "ね");
        add("no", "の");
        add("ha", "は");
        add("hi", "ひ");
        add("hu", "ふ");
        add("he", "へ");
        add("ho", "ほ");
        add("ma", "ま");
        add("mi", "み");
        add("mu", "む");
        add("me", "め");
        add("mo", "も");
        add("ya", "や");
        add("yi", "い");
        add("yu", "ゆ");
        add("ye", "いぇ");
        add("yo", "よ");
        add("ra", "ら");
        add("ri", "り");
        add("ru", "る");
        add("re", "れ");
        add("ro", "ろ");
        add("wa", "わ");
        add("wi", "うぃ");
        add("wu", "う");
        add("we", "うぇ");
        add("wo", "を");
        add("nn", "ん");
        add("ga", "が");
        add("gi", "ぎ");
        add("gu", "ぐ");
        add("ge", "げ");
        add("go", "ご");
        add("za", "ざ");
        add("zi", "じ");
        add("zu", "ず");
        add("ze", "ぜ");
        add("zo", "ぞ");
        add("da", "だ");
        add("di", "ぢ");
        add("du", "づ");
        add("de", "で");
        add("do", "ど");
        add("ja", "じゃ");
        add("ji", "じ");
        add("ju", "じゅ");
        add("je", "じぇ");
        add("jo", "じょ");
        add("ba", "ば");
        add("bi", "び");
        add("bu", "ぶ");
        add("be", "べ");
        add("bo", "ぼ");
        add("pa", "ぱ");
        add("pi", "ぴ");
        add("pu", "ぷ");
        add("pe", "ぺ");
        add("po", "ぽ");
        add("kya", "きゃ");
        add("kyi", "きぃ");
        add("kyu", "きゅ");
        add("kye", "きぇ");
        add("kyo", "きょ");
        add("sha", "しゃ");
        add("shi", "し");
        add("shu", "しゅ");
        add("she", "しぇ");
        add("sho", "しょ");
        add("cha", "ちゃ");
        add("chi", "ち");
        add("chu", "ちゅ");
        add("che", "ちぇ");
        add("cho", "ちょ");
        add("tya", "ちゃ");
        add("tyi", "ちぃ");
        add("tyu", "ちゅ");
        add("tye", "ちぇ");
        add("tyo", "ちょ");
        add("gya", "ぎゃ");
        add("gyi", "ぎぃ");
        add("gyu", "ぎゅ");
        add("gye", "ぎぇ");
        add("gyo", "ぎょ");
        add("bya", "びゃ");
        add("byi", "びぃ");
        add("byu", "びゅ");
        add("bye", "びぇ");
        add("byo", "びょ");
        add("nya", "にゃ");
        add("nyi", "にぃ");
        add("nyu", "にゅ");
        add("nye", "にぇ");
        add("nyo", "にょ");
        add("hya", "ひゃ");
        add("hyi", "ひぃ");
        add("hyu", "ひゅ");
        add("hye", "ひぇ");
        add("hyo", "ひょ");
        add("mya", "みゃ");
        add("myi", "みぃ");
        add("myu", "みゅ");
        add("mye", "みぇ");
        add("myo", "みょ");
        add("pya", "ぴゃ");
        add("pyi", "ぴぃ");
        add("pyu", "ぴゅ");
        add("pye", "ぴぇ");
        add("pyo", "ぴょ");
        add("rya", "りゃ");
        add("ryi", "りぃ");
        add("ryu", "りゅ");
        add("rye", "りぇ");
        add("ryo", "りょ");
        add("fa", "ふぁ");
        add("fi", "ふぃ");
        add("fu", "ふ");
        add("fe", "ふぇ");
        add("fo", "ふぉ");
        add("va", "ヴぁ");
        add("vi", "ヴぃ");
        add("vu", "ヴ");
        add("ve", "ヴぇ");
        add("vo", "ヴぉ");
        add("tsa", "つぁ");
        add("tsi", "つぃ");
        add("tsu", "つ");
        add("tse", "つぇ");
        add("tso", "つぉ");
        add("jya", "じゃ");
        add("jyi", "じぃ");
        add("jyu", "じゅ");
        add("jye", "じぇ");
        add("jyo", "じょ");
        add("xtu", "っ");
        add("ltu", "っ");
        add("xa", "ぁ");
        add("xi", "ぃ");
        add("xu", "ぅ");
        add("xe", "ぇ");
        add("xo", "ぉ");
        add("la", "ぁ");
        add("li", "ぃ");
        add("lu", "ぅ");
        add("le", "ぇ");
        add("lo", "ぉ");
        add("xka", "ヵ");
        add("xke", "ヶ");
        add("lka", "ヵ");
        add("lke", "ヶ");
        add("sya", "しゃ");
        add("syi", "しぃ");
        add("syu", "しゅ");
        add("sye", "しぇ");
        add("syo", "しょ");
        add("zya", "じゃ");
        add("zyi", "じぃ");
        add("zyu", "じゅ");
        add("zye", "じぇ");
        add("zyo", "じょ");
        add("dda", "っだ");
        add("ddi", "っぢ");
        add("ddu", "っづ");
        add("dde", "っで");
        add("ddo", "っど");
        add("tta", "った");
        add("tti", "っち");
        add("ttu", "っつ");
        add("tte", "って");
        add("tto", "っと");
        add("kka", "っか");
        add("kki", "っき");
        add("kku", "っく");
        add("kke", "っけ");
        add("kko", "っこ");
        add("ssa", "っさ");
        add("ssi", "っし");
        add("ssu", "っす");
        add("sse", "っせ");
        add("sso", "っそ");
        add("mma", "っま");
        add("mmi", "っみ");
        add("mmu", "っむ");
        add("mme", "っめ");
        add("mmo", "っも");
        add("lya", "ゃ");
        add("lyi", "ぃ");
        add("lyu", "ゅ");
        add("lye", "ぇ");
        add("lyo", "ょ");
        add("xya", "ゃ");
        add("xyi", "ぃ");
        add("xyu", "ゅ");
        add("xye", "ぇ");
        add("xyo", "ょ");
        add("tha", "てゃ");
        add("thi", "てぃ");
        add("thu", "てゅ");
        add("the", "てぇ");
        add("tho", "てょ");
        add("ttya", "っちゃ");
        add("ttyi", "っちぃ");
        add("ttyu", "っちゅ");
        add("ttye", "っちぇ");
        add("ttyo", "っちょ");
        add("ccha", "っちゃ");
        add("cchi", "っちぃ");
        add("cchu", "っちゅ");
        add("cche", "っちぇ");
        add("ccho", "っちょ");
        add("ssha", "っしゃ");
        add("sshi", "っし");
        add("sshu", "っしゅ");
        add("sshe", "っしぇ");
        add("ssho", "っしょ");
        add("cya", "ちゃ");
        add("cyi", "ちぃ");
        add("cyu", "ちゅ");
        add("cye", "ちぇ");
        add("cyo", "ちょ");
        add("-", "ー");
        add("n", "ん"); // fallback
    }
}