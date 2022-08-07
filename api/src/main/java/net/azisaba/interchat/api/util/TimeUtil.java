package net.azisaba.interchat.api.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class TimeUtil {
    private TimeUtil() { throw new AssertionError(); }

    @Contract(pure = true)
    public static @NotNull String toRelativeTimeAbs(long from, long to) {
        long abs = Math.abs(from - to);
        long originalAbs = abs;
        List<String> sb = new ArrayList<>();
        if (abs >= 1000 * 60 * 60 * 24) {
            long days = abs / (1000 * 60 * 60 * 24);
            sb.add(days + "d");
            abs -= days * (1000 * 60 * 60 * 24);
        }
        if (abs >= 1000 * 60 * 60) {
            long hours = abs / (1000 * 60 * 60);
            sb.add(hours + "h");
            abs -= hours * (1000 * 60 * 60);
        }
        if (abs >= 1000 * 60) {
            long minutes = abs / (1000 * 60);
            sb.add(minutes + "m");
            abs -= minutes * (1000 * 60);
        }
        if (abs >= 1000) {
            long seconds = abs / 1000;
            sb.add(seconds + "s");
            abs -= seconds * 1000;
        }
        if (originalAbs < 1000/*abs > 0*/) {
            sb.add(abs + "ms");
        }
        return String.join(" ", sb);
    }
}
