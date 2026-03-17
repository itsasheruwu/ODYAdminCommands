package com.odyadmincommands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

public final class MayFlyCommand implements CommandExecutor {

    private static final String FLIGHT_PERMISSION = "odyadmincommands.mayfly";
    private static final String FLIGHT_OTHERS_PERMISSION = "odyadmincommands.mayfly.others";

    private final ODYAdminCommandsPlugin plugin;

    public MayFlyCommand(ODYAdminCommandsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(FLIGHT_PERMISSION)) {
            plugin.getMessageHelper().send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("Usage: /mayfly <player> [on|off|toggle]");
            return true;
        }

        String targetName = args[0];
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            plugin.getMessageHelper().send(sender, "player-not-found");
            return true;
        }

        UUID targetUuid = targetPlayer.getUniqueId();
        String displayName = targetPlayer.getName();
        Player commandPlayer = sender instanceof Player ? (Player) sender : null;
        boolean targetingOther = commandPlayer == null || !commandPlayer.getUniqueId().equals(targetUuid);

        if (targetingOther && !sender.hasPermission(FLIGHT_OTHERS_PERMISSION)) {
            plugin.getMessageHelper().send(sender, "no-permission-others");
            return true;
        }

        String configuredDefaultMode = plugin.getConfig().getString("mayfly.default-mode", "toggle");
        if (configuredDefaultMode == null || configuredDefaultMode.isBlank()) {
            configuredDefaultMode = "toggle";
        }
        String modeArg = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : configuredDefaultMode.toLowerCase(Locale.ROOT);

        boolean newState;
        switch (modeArg) {
            case "on" -> newState = true;
            case "off" -> newState = false;
            case "toggle" -> newState = !plugin.getStorageManager().isFlightEnabled(targetUuid);
            default -> {
                plugin.getMessageHelper().send(sender, "invalid-mode");
                return true;
            }
        }

        plugin.getStorageManager().setFlightEnabled(targetUuid, newState);

        if (targetPlayer.isOnline()) {
            if (newState) {
                FlightHelper.enable(targetPlayer, plugin);
            } else {
                FlightHelper.disable(targetPlayer);
            }
        }

        String executorName = sender instanceof Player ? sender.getName() : "Console";
        if (newState) {
            plugin.getMessageHelper().send(sender, "enabled", displayName);
            if (targetPlayer.isOnline() && targetPlayer != sender) {
                plugin.getMessageHelper().send(targetPlayer, "target-received-enabled", displayName, executorName);
            }
        } else {
            plugin.getMessageHelper().send(sender, "disabled", displayName);
            if (targetPlayer.isOnline() && targetPlayer != sender) {
                plugin.getMessageHelper().send(targetPlayer, "target-received-disabled", displayName, executorName);
            }
        }

        plugin.getLogger().info(String.format("[MayFly] Flight %s for %s (%s) by %s",
                newState ? "enabled" : "disabled",
                displayName,
                targetUuid,
                executorName));

        return true;
    }
}
