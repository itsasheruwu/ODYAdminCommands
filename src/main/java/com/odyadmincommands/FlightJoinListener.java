package com.odyadmincommands;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class FlightJoinListener implements Listener {

    private final ODYAdminCommandsPlugin plugin;

    public FlightJoinListener(ODYAdminCommandsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        if (plugin.getStorageManager().isFlightEnabled(player.getUniqueId())) {
            FlightHelper.enable(player, plugin);
        }
    }
}
