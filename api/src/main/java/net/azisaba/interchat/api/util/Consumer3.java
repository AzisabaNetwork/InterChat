package net.azisaba.interchat.api.util;

public interface Consumer3<K1, K2, K3> {
    void accept(K1 k1, K2 k2, K3 k3);
}
