package com.odyadmincommands;

import java.util.function.Function;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ToggleStateCommand implements CommandExecutor {
    private final String usage;
    private final Function<Player, Boolean> toggleAction;
    private final Function<Boolean, String> feedbackMessage;

    public ToggleStateCommand(String usage, Function<Player, Boolean> toggleAction, Function<Boolean, String> feedbackMessage) {
        this.usage = usage;
        this.toggleAction = toggleAction;
        this.feedbackMessage = feedbackMessage;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendPlainMessage("Only players can use this command.");
            return true;
        }

        if (args.length != 0) {
            player.sendPlainMessage("Usage: " + this.usage);
            return true;
        }

        boolean enabled = this.toggleAction.apply(player);
        player.sendPlainMessage(this.feedbackMessage.apply(enabled));
        return true;
    }
}
