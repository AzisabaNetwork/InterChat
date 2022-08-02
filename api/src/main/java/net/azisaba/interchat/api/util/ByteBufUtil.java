package net.azisaba.interchat.api.util;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

public final class ByteBufUtil {
    private ByteBufUtil() { throw new AssertionError(); }

    public static byte @NotNull [] toByteArray(@NotNull ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), bytes);
        buf.release();
        return bytes;
    }
}
