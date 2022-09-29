package net.azisaba.interchat.api.util;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface QueryExecutor {
    void query(@Language("SQL") @NotNull String sql, @NotNull SQLThrowableConsumer<PreparedStatement> action) throws SQLException;
}
