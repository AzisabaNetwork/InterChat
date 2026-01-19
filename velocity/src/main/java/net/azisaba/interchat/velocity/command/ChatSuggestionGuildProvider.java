package net.azisaba.interchat.velocity.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.velocitypowered.api.command.CommandSource;
import net.azisaba.interchat.api.guild.Guild;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface ChatSuggestionGuildProvider {
    @Nullable Guild apply(@NotNull CommandContext<CommandSource> ctx, @NotNull UUID uuid) throws CommandSyntaxException;
}
