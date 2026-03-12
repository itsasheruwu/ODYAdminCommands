package com.odyadmincommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class CleanLogsCommand implements CommandExecutor {
    private final LogCleanupService logCleanupService;

    public CleanLogsCommand(LogCleanupService logCleanupService) {
        this.logCleanupService = logCleanupService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 0) {
            sender.sendPlainMessage("Usage: /cleanlogs");
            return true;
        }

        LogCleanupService.CleanupResult result = this.logCleanupService.clean();
        sender.sendPlainMessage(
            "Cleaned logs: "
                + result.deletedLogFiles()
                + ", crash logs: "
                + result.deletedCrashLogs()
                + (result.success() ? "." : " (some files could not be removed).")
        );
        return true;
    }
}
