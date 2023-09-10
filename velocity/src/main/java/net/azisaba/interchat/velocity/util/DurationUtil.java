package net.azisaba.interchat.velocity.util;

import com.velocitypowered.api.command.CommandSource;
import net.azisaba.interchat.velocity.text.VMessages;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;

public class DurationUtil {
    public static @NotNull Duration convertStringToDuration(@NotNull String str) throws IllegalArgumentException {
        str = str.toLowerCase(Locale.ROOT);

        Duration duration = Duration.ofSeconds(0);
        while (!str.isEmpty()) {
            List<Integer> indexes =
                    new ArrayList<>(Arrays.asList(str.indexOf("d"), str.indexOf("h"), str.indexOf("m"), str.indexOf("s")));
            indexes.removeIf(i -> i < 0);

            OptionalInt minIndexOptional = indexes.stream().mapToInt(i -> i).min();
            if (minIndexOptional.isEmpty()) {
                throw new IllegalArgumentException();
            }

            int minIndex = minIndexOptional.getAsInt();
            String numStr = str.substring(0, minIndex);
            if (numStr.isEmpty()) {
                throw new IllegalArgumentException();
            }

            int amount;
            try {
                amount = Integer.parseInt(numStr);
                if (amount < 0) {
                    throw new IllegalArgumentException();
                }
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException();
            }

            char unit = str.charAt(minIndex);
            if (unit == 'd') {
                duration = duration.plusDays(amount);
            } else if (unit == 'h') {
                duration = duration.plusHours(amount);
            } else if (unit == 'm') {
                duration = duration.plusMinutes(amount);
            } else if (unit == 's') {
                duration = duration.plusSeconds(amount);
            }

            str = str.substring(minIndex + 1);
        }

        return duration;
    }

    public static @NotNull String convertDurationToString(@NotNull CommandSource source, @NotNull Duration duration) {
        String str = "";
        if (duration.toDays() > 0) {
            if (duration.toDays() > 1) {
                str += VMessages.format(source, "datetime.days", duration.toDays());
            } else {
                str += VMessages.format(source, "datetime.day", duration.toDays());
            }
            duration = duration.minusDays(duration.toDays());
        }
        if (duration.toHours() > 0) {
            if (duration.toHours() > 1) {
                str += VMessages.format(source, "datetime.hours", duration.toHours());
            } else {
                str += VMessages.format(source, "datetime.hour", duration.toHours());
            }
            duration = duration.minusHours(duration.toHours());
        }
        if (duration.toMinutes() > 0) {
            if (duration.toMinutes() > 1) {
                str += VMessages.format(source, "datetime.minutes", duration.toMinutes());
            } else {
                str += VMessages.format(source, "datetime.minute", duration.toMinutes());
            }
            duration = duration.minusMinutes(duration.toMinutes());
        }
        if (duration.getSeconds() > 0) {
            if (duration.getSeconds() > 1) {
                str += VMessages.format(source, "datetime.seconds", duration.getSeconds());
            } else {
                str += VMessages.format(source, "datetime.second", duration.getSeconds());
            }
            //duration = duration.minusSeconds(duration.getSeconds());
        }

        return str.trim();
    }
}
