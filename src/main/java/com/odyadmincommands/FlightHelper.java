package com.odyadmincommands;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public final class FlightHelper {

    private static final double MIN_SPEED = 0.0;
    private static final double MAX_SPEED = 1.0;

    private FlightHelper() {
    }

    public static void enable(Player player, ODYAdminCommandsPlugin plugin) {
        player.setAllowFlight(true);
        GameMode mode = player.getGameMode();
        if (mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE) {
            player.setFlying(true);
        }
        double speed = plugin.getConfig().getDouble("mayfly.flight-speed", 0.1);
        speed = Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed));
        player.setFlySpeed((float) speed);
    }

    public static void disable(Player player) {
        if (player.isFlying()) {
            player.setFlying(false);
        }
        GameMode mode = player.getGameMode();
        if (mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE) {
            player.setAllowFlight(false);
        }
    }
}
