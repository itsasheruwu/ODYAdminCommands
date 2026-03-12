package com.odyadmincommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ConfirmChatCommand implements CommandExecutor {
    private final VanishService vanishService;

    public ConfirmChatCommand(VanishService vanishService) {
        this.vanishService = vanishService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendPlainMessage("Only players can use this command.");
            return true;
        }

        if (args.length != 1) {
            player.sendPlainMessage("That chat confirmation is no longer valid.");
            return true;
        }

        boolean resent = this.vanishService.confirmPendingChat(player, args[0]);
        if (!resent) {
            player.sendPlainMessage("That chat confirmation is no longer valid.");
        }
        return true;
    }
}
