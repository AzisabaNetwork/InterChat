package net.azisaba.interchat.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CompoundUserDataProvider implements UserDataProvider {
    private final List<UserDataProvider> providers;

    /**
     * Creates a new instance of {@link CompoundUserDataProvider}. First one is called at first, and the last one is
     * called at last. In other words, prefixes/suffixes that first provider provided may be overwritten by later providers.
     * @param providers the list of providers
     */
    public CompoundUserDataProvider(@NotNull UserDataProvider @NotNull ... providers) {
        this.providers = Arrays.asList(providers);
    }

    /**
     * Creates a new instance of {@link CompoundUserDataProvider}. First one is called at first, and the last one is
     * called at last. In other words, prefixes/suffixes that first provider provided may be overwritten by later providers.
     * @param providers the list of providers
     */
    public CompoundUserDataProvider(@NotNull List<? extends @NotNull UserDataProvider> providers) {
        this.providers = Collections.unmodifiableList(providers);
    }

    @UnmodifiableView
    public @NotNull List<@NotNull UserDataProvider> getProviders() {
        return providers;
    }

    @Override
    public @NotNull Map<@NotNull String, @NotNull String> getPrefix(@NotNull UUID uuid) {
        Map<String, String> map = new HashMap<>();
        for (UserDataProvider provider : providers) {
            map.putAll(provider.getPrefix(uuid));
        }
        return map;
    }

    @Override
    public @NotNull Map<@NotNull String, @NotNull String> getSuffix(@NotNull UUID uuid) {
        Map<String, String> map = new HashMap<>();
        for (UserDataProvider provider : providers) {
            map.putAll(provider.getSuffix(uuid));
        }
        return map;
    }

    @Override
    public @NotNull CompletableFuture<Void> requestUpdate(@NotNull UUID uuid, @NotNull String server) {
        List<CompletableFuture<?>> cfs = new ArrayList<>();
        for (UserDataProvider p : providers) {
            cfs.add(p.requestUpdate(uuid, server));
        }
        return CompletableFuture.allOf(cfs.toArray(new CompletableFuture[0]));
    }
}
