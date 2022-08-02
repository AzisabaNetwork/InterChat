package net.azisaba.interchat.api.network;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum Side {
    PROXY,
    BACKEND,
    ;

    @Contract(pure = true)
    @NotNull
    public Side getOpposite() {
        switch (this) {
            case PROXY: return BACKEND;
            case BACKEND: return PROXY;
            default: throw new AssertionError(this);
        }
    }
}
