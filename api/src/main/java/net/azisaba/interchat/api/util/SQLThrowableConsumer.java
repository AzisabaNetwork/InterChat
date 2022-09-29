package net.azisaba.interchat.api.util;

import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public interface SQLThrowableConsumer<T> {
    void accept(@NotNull T t) throws SQLException;
}
