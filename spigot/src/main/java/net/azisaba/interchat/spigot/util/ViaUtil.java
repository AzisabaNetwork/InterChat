package net.azisaba.interchat.spigot.util;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ViaUtil {
    public static Object getViaAPI() throws ReflectiveOperationException {
        return Class.forName("com.viaversion.viaversion.api.Via")
                .getMethod("getAPI")
                .invoke(null);
    }

    public static int getPlayerVersion(@NotNull Object viaApi, @NotNull UUID player) throws ReflectiveOperationException {
        return (int) Class.forName("com.viaversion.viaversion.api.ViaAPI")
                .getMethod("getPlayerVersion", UUID.class)
                .invoke(viaApi, player);
    }
}
