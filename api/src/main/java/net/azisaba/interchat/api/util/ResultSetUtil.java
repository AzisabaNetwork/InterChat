package net.azisaba.interchat.api.util;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class ResultSetUtil {
    public static <T> @NotNull List<T> toList(@NotNull ResultSet rs, @NotNull Function<ResultSet, T> constructor) throws SQLException {
        List<T> list = new ArrayList<>();
        while (rs.next()) {
            list.add(constructor.apply(rs));
        }
        return list;
    }
}
