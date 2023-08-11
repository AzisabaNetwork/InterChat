package net.azisaba.interchat.api.data;

import net.luckperms.api.LuckPerms;
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
import java.util.stream.Collectors;

public class LuckPermsUserDataProvider implements UserDataProvider {
    public static boolean isAvailable() {
        try {
            Class.forName("net.luckperms.api.LuckPermsProvider");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    public static <T extends ChatMetaNode<?, ?>> @NotNull List<@NotNull ChatMetaNodeData> getChatMetaNodeDataList(@NotNull UUID uuid, @NotNull Class<T> clazz) {
        List<ChatMetaNodeData> list = new ArrayList<>();
        LuckPerms api = LuckPermsProvider.get();
        User user = api.getUserManager().loadUser(uuid).join();
        List<Map.Entry<String, Set<String>>> groupServerMap = new ArrayList<>();
        for (Node node : user.data().toCollection()) {
            if (node.getKey().startsWith("group.")) {
                String groupName = node.getKey().substring("group.".length());
                Set<String> servers = node.getContexts().getValues("server");
                if (servers.isEmpty()) {
                    groupServerMap.add(new AbstractMap.SimpleImmutableEntry<>(groupName, Collections.emptySet()));
                    continue;
                }
                try {
                    groupServerMap.add(new AbstractMap.SimpleImmutableEntry<>(groupName, servers));
                } catch (UnsupportedOperationException ignored) {
                }
            } else if (clazz.isInstance(node)) {
                // node assigned to user
                Set<String> servers = node.getContexts().getValues("server");
                int priority = ((ChatMetaNode<?, ?>) node).getPriority();
                String metaValue = ((ChatMetaNode<?, ?>) node).getMetaValue();
                list.add(new ChatMetaNodeData(servers, priority, metaValue, null));
            }
        }
        for (Map.Entry<String, Set<String>> entry : new ArrayList<>(groupServerMap)) {
            api.getGroupManager().loadGroup(entry.getKey()).join().ifPresent(group ->
                    group.getInheritedGroups(QueryOptions.builder(QueryMode.NON_CONTEXTUAL).build())
                            .forEach(inheritedGroup -> groupServerMap.add(new AbstractMap.SimpleImmutableEntry<>(inheritedGroup.getName(), entry.getValue()))));
        }
        user.getInheritedGroups(QueryOptions.builder(QueryMode.NON_CONTEXTUAL).flag(Flag.RESOLVE_INHERITANCE, true).build())
                .forEach(group -> {
                    int groupWeight = group.getWeight().orElse(0);
                    for (Node node : group.data().toCollection()) {
                        if (clazz.isInstance(node)) {
                            // prefix/suffix node assigned to group
                            Set<String> servers = new HashSet<>(node.getContexts().getValues("server"));
                            Set<String> groupServer = new HashSet<>();
                            for (Map.Entry<String, Set<String>> entry : groupServerMap
                                    .stream()
                                    .filter(entry -> entry.getKey().equals(group.getName()))
                                    .sorted(Comparator.comparingInt(entry -> entry.getValue().size()))
                                    .collect(Collectors.toList())
                            ) {
                                if (entry.getValue().isEmpty()) {
                                    // user inherits the group without server= context
                                    groupServer.clear();
                                    break;
                                }
                                // user inherits the group with server= context
                                groupServer.addAll(entry.getValue());
                            }
                            if (!groupServer.isEmpty()) {
                                if (!servers.isEmpty()) {
                                    servers.retainAll(groupServer);
                                    if (servers.isEmpty()) {
                                        // example: user inherits the group with server=srv1 and group inherits meta with server=srv2
                                        servers = null;
                                    }
                                } else {
                                    servers.addAll(groupServer);
                                }
                            }
                            int priority = ((ChatMetaNode<?, ?>) node).getPriority();
                            String metaValue = ((ChatMetaNode<?, ?>) node).getMetaValue();
                            list.add(new ChatMetaNodeData(servers, priority, metaValue, groupWeight));
                        }
                    }
                });
        return list;
    }

    public static <T extends ChatMetaNode<?, ?>> @NotNull Map<@NotNull String, @NotNull String> getServerToMetaValueMap(@NotNull UUID uuid, @NotNull Class<T> clazz) {
        return ChatMetaNodeData.toMap(getChatMetaNodeDataList(uuid, clazz));
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
