package com.odyadmincommands;

import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class SilentCommandListener implements Listener {
    private final PluginCommand cleanLogsCommand;

    public SilentCommandListener(PluginCommand cleanLogsCommand) {
        this.cleanLogsCommand = cleanLogsCommand;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (!message.regionMatches(true, 0, "/cleanlogs", 0, "/cleanlogs".length())) {
            return;
        }
        if (message.length() > "/cleanlogs".length() && !Character.isWhitespace(message.charAt("/cleanlogs".length()))) {
            return;
        }

        event.setCancelled(true);
        dispatchSilently(event.getPlayer(), message.startsWith("/") ? message.substring(1) : message);
    }

    private void dispatchSilently(Player player, String commandLine) {
        String[] arguments = commandLine.split("\\s+");
        String label = arguments[0];
        String[] args = new String[Math.max(0, arguments.length - 1)];
        if (arguments.length > 1) {
            System.arraycopy(arguments, 1, args, 0, args.length);
        }

        if (!this.cleanLogsCommand.testPermission(player)) {
            return;
        }

        CommandExecutorAdapter executor = new CommandExecutorAdapter(this.cleanLogsCommand);
        executor.execute(player, label, args);
    }

    private record CommandExecutorAdapter(PluginCommand command) {
        private void execute(Player player, String label, String[] args) {
            if (this.command.getExecutor() == null) {
                return;
            }
            this.command.getExecutor().onCommand(player, this.command, label, args);
        }
    }
}
