package com.odyadmincommands;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ProtocolLibCleanLogsIntegration {
    private final JavaPlugin plugin;
    private final PluginCommand cleanLogsCommand;

    public ProtocolLibCleanLogsIntegration(JavaPlugin plugin, PluginCommand cleanLogsCommand) {
        this.plugin = plugin;
        this.cleanLogsCommand = cleanLogsCommand;
    }

    public void register() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
            this.plugin,
            ListenerPriority.HIGHEST,
            PacketType.Play.Client.CHAT_COMMAND,
            PacketType.Play.Client.CHAT_COMMAND_SIGNED
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                String commandName = event.getPacket().getStrings().readSafely(0);
                if (commandName == null || !commandName.equalsIgnoreCase("cleanlogs")) {
                    return;
                }

                event.setCancelled(true);
                Player player = event.getPlayer();
                Bukkit.getScheduler().runTask(ProtocolLibCleanLogsIntegration.this.plugin, () -> dispatchSilently(player, commandName));
            }
        });
    }

    private void dispatchSilently(Player player, String label) {
        if (!player.isOnline()) {
            return;
        }
        if (!this.cleanLogsCommand.testPermission(player)) {
            return;
        }

        if (this.cleanLogsCommand.getExecutor() != null) {
            this.cleanLogsCommand.getExecutor().onCommand(player, this.cleanLogsCommand, label, new String[0]);
        }
    }
}
