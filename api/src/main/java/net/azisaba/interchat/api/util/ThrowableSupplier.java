package net.azisaba.interchat.api.util;

public interface ThrowableSupplier<T> {
    T get() throws Throwable;
}
