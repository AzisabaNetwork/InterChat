package net.azisaba.interchat.api.data;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.ChatMetaNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.query.Flag;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class LuckPermsUserDataProvider implements UserDataProvider {
    public static boolean isAvailable() {
        try {
            Class.forName("net.luckperms.api.LuckPermsProvider");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    public static <T extends ChatMetaNode<?, ?>> @NotNull Map<@NotNull String, @NotNull String> getServerToMetaValueMap(@NotNull UUID uuid, @NotNull Class<T> clazz) {
        List<ChatMetaNodeData> list = new ArrayList<>();
        User user = LuckPermsProvider.get().getUserManager().loadUser(uuid).join();
        Map<String, Set<String>> groupServerMap = new HashMap<>();
        for (Node node : user.data().toCollection()) {
            if (node.getKey().startsWith("group.")) {
                String groupName = node.getKey().substring("group.".length());
                Set<String> servers = node.getContexts().getValues("server");
                if (servers.isEmpty()) {
                    groupServerMap.put(groupName, Collections.emptySet());
                    continue;
                }
                try {
                    groupServerMap.computeIfAbsent(groupName, k -> new HashSet<>()).addAll(servers);
                } catch (UnsupportedOperationException ignored) {}
            } else if (clazz.isInstance(node)) {
                // node assigned to user
                Set<String> servers = node.getContexts().getValues("server");
                int priority = ((ChatMetaNode<?, ?>) node).getPriority();
                String metaValue = ((ChatMetaNode<?, ?>) node).getMetaValue();
                list.add(new ChatMetaNodeData(servers, priority, metaValue, null));
            }
        }
        user.getInheritedGroups(QueryOptions.builder(QueryMode.NON_CONTEXTUAL).flag(Flag.RESOLVE_INHERITANCE, true).build())
                .forEach(group -> {
                    int groupWeight = group.getWeight().orElse(0);
                    for (Node node : group.data().toCollection()) {
                        if (clazz.isInstance(node)) {
                            // node assigned to group
                            Set<String> servers = new HashSet<>(node.getContexts().getValues("server"));
                            Set<String> groupServer = groupServerMap.getOrDefault(group.getName(), Collections.emptySet());
                            if (!servers.isEmpty() && !groupServer.isEmpty()) {
                                servers.removeIf(s -> !groupServer.contains(s));
                                if (servers.isEmpty()) return; // example: user inherits the group with server=srv1 and group inherits meta with server=srv2
                            } else if (servers.isEmpty() && !groupServer.isEmpty()) {
                                servers.addAll(groupServer);
                            }
                            int priority = ((ChatMetaNode<?, ?>) node).getPriority();
                            String metaValue = ((ChatMetaNode<?, ?>) node).getMetaValue();
                            list.add(new ChatMetaNodeData(servers, priority, metaValue, groupWeight));
                        }
                    }
                });
        return ChatMetaNodeData.toMap(list);
    }

    @Override
    public @NotNull Map<@NotNull String, @NotNull String> getPrefix(@NotNull UUID uuid) {
        return getServerToMetaValueMap(uuid, PrefixNode.class);
    }

    @Override
    public @NotNull Map<@NotNull String, @NotNull String> getSuffix(@NotNull UUID uuid) {
        return getServerToMetaValueMap(uuid, SuffixNode.class);
    }
}
