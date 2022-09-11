package net.azisaba.interchat.api.text;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class StringReader {
    private final String text;
    private int index = 0;

    public StringReader(@NotNull String text) {
        this.text = Objects.requireNonNull(text, "text");
    }

    public char peek() {
        return text.charAt(index);
    }

    @NotNull
    public String peekRemaining() {
        return text.substring(index);
    }

    @NotNull
    public String read(int amount) {
        String result = text.substring(index, index + amount);
        index += amount;
        return result;
    }

    public boolean startsWith(@NotNull String prefix) {
        return peekRemaining().startsWith(prefix);
    }

    public void skip(int amount) {
        index += amount;
    }

    public boolean isEOF() {
        return index >= text.length();
    }
}
