package net.azisaba.interchat.spigot.commands;

import net.azisaba.interchat.spigot.listener.PlayerListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SpigotInterChatCommand implements TabExecutor {
    private static final List<String> COMMANDS = Arrays.asList("clearCache");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }
        if (args[0].equals("clearCache")) {
            PlayerListener.clearCache();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return COMMANDS.stream().filter(s -> s.startsWith(args[0])).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
