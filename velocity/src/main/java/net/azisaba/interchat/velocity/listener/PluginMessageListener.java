package net.azisaba.interchat.velocity.listener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import net.azisaba.interchat.api.Logger;

public class PluginMessageListener {
    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        if (!(e.getSource() instanceof ServerConnection)) {
            return;
        }
        if (!e.getIdentifier().getId().equalsIgnoreCase("InterChat") && !e.getIdentifier().getId().equals("interchat:main")) {
            return;
        }
        e.setResult(PluginMessageEvent.ForwardResult.handled());
        ByteArrayDataInput input = ByteStreams.newDataInput(e.getData());
        String command = input.readUTF();
        Logger.getCurrentLogger().info("Received {} packet from {} / {}", command, e.getSource(), ((ServerConnection) e.getSource()).getServerInfo());
        if (command.equals("Hello")) {
            ChatListener.FORWARD_TO_BACKEND.add(((ServerConnection) e.getSource()).getServerInfo().getName());
        }
    }
}
