package net.azisaba.interchat.api.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class ChatMetaNodeData {
    private final @NotNull Set<@NotNull String> servers;
    private final int priority;
    private final @NotNull String metaValue;
    private final @Nullable Integer groupWeight;

    @Contract(pure = true)
    public ChatMetaNodeData(@NotNull Set<@NotNull String> servers, int priority, @NotNull String metaValue, @Nullable Integer groupWeight) {
        this.servers = servers;
        this.priority = priority;
        this.metaValue = metaValue;
        this.groupWeight = groupWeight;
    }

    @Contract(pure = true)
    public @NotNull Set<@NotNull String> getServers() {
        return servers;
    }

    @Contract(pure = true)
    public int getPriority() {
        return priority;
    }

    @Contract(pure = true)
    public @NotNull String getMetaValue() {
        return metaValue;
    }

    public @Nullable Integer getGroupWeight() {
        return groupWeight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatMetaNodeData)) return false;
        ChatMetaNodeData that = (ChatMetaNodeData) o;
        return getPriority() == that.getPriority() && Objects.equals(getServers(), that.getServers()) && Objects.equals(getMetaValue(), that.getMetaValue()) && Objects.equals(getGroupWeight(), that.getGroupWeight());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getServers(), getPriority(), getMetaValue(), getGroupWeight());
    }

    public static @NotNull Map<@NotNull String, @NotNull String> toMap(@NotNull List<ChatMetaNodeData> dataList) {
        Map<String, List<ChatMetaNodeData>> nodeMap = new HashMap<>();
        for (ChatMetaNodeData data : dataList) {
            if (data.servers.isEmpty()) {
                nodeMap.computeIfAbsent("global", k -> new ArrayList<>()).add(data);
            } else {
                for (String server : data.servers) {
                    nodeMap.computeIfAbsent(server, k -> new ArrayList<>()).add(data);
                }
            }
        }
        Map<String, String> map = new HashMap<>();
        nodeMap.forEach((server, list) -> {
            int maxPriority = list.stream().mapToInt(ChatMetaNodeData::getPriority).max().orElse(0);
            list.removeIf(data -> data.priority != maxPriority);
            if (list.size() == 1) {
                map.put(server, list.get(0).getMetaValue());
                return;
            }
            list.sort(Comparator.comparingInt(a -> a.groupWeight != null ? a.groupWeight : Integer.MAX_VALUE));
            map.put(server, list.get(list.size() - 1).getMetaValue());
        });
        return map;
    }
}
