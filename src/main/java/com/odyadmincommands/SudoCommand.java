package com.odyadmincommands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SudoCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendPlainMessage("Usage: /sudo <player> <command>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendPlainMessage("Player '" + args[0] + "' is not online.");
            return true;
        }

        String commandLine = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        if (commandLine.startsWith("/")) {
            commandLine = commandLine.substring(1);
        }

        if (commandLine.isBlank()) {
            sender.sendPlainMessage("Usage: /sudo <player> <command>");
            return true;
        }

        boolean executed = Bukkit.dispatchCommand(target, commandLine);
        if (!executed) {
            sender.sendPlainMessage("Failed to run command as " + target.getName() + ": /" + commandLine);
            return true;
        }

        sender.sendPlainMessage("Ran as " + target.getName() + ": /" + commandLine);
        return true;
    }
}
