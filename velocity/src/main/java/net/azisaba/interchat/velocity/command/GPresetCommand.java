package net.azisaba.interchat.velocity.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.azisaba.interchat.api.guild.Guild;
import net.azisaba.interchat.velocity.VelocityPlugin;
import net.azisaba.interchat.velocity.text.VMessages;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GPresetCommand extends AbstractCommand {
    private final VelocityPlugin plugin;
    private final int presetNumber;

    public GPresetCommand(@NotNull VelocityPlugin plugin, int presetNumber) {
        this.plugin = plugin;
        this.presetNumber = presetNumber;
    }

    @Override
    protected @NotNull LiteralArgumentBuilder<CommandSource> createBuilder() {
        String commandName = "g" + presetNumber;
        return literal(commandName)
                .requires(source -> source instanceof Player && source.hasPermission("interchat.guild") && source.hasPermission("interchat.guild.chat"))
                .executes(ctx -> executeFocus((Player) ctx.getSource(), GuildCommand.getPresetGuildForSuggestion(((Player) ctx.getSource()).getUniqueId(), presetNumber)))
                .then(argument("message", StringArgumentType.greedyString())
                        .suggests(GuildCommand.getChatSuggestionProvider(plugin.getJedisBox(), (ctx, uuid) ->
                                GuildCommand.getPresetGuildForSuggestion(uuid, presetNumber)))
                        .executes(ctx -> GuildCommand.executePresetChat(
                                (Player) ctx.getSource(),
                                presetNumber,
                                StringArgumentType.getString(ctx, "message")
                        ))
                );
    }

    private int executeFocus(@NotNull Player player, @Nullable Guild guild) {
        if (guild == null) {
            player.sendMessage(VMessages.formatComponent(player, "command.guild.preset.not_set", presetNumber, GuildCommand.COMMAND_NAME, presetNumber).color(NamedTextColor.RED));
            return 0;
        }
        player.sendMessage(VMessages.formatComponent(player, "command.guild.focus.focused", guild.name()).color(NamedTextColor.GREEN));
        return 1;
    }
}
