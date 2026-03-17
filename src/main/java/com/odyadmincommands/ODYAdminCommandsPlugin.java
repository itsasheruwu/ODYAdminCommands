package com.odyadmincommands;

import java.util.Locale;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ODYAdminCommandsPlugin extends JavaPlugin {
    private VanishService vanishService;
    private AutoUpdateService autoUpdateService;
    private LogCleanupService logCleanupService;
    private FlightStorageManager flightStorageManager;
    private MessageHelper messageHelper;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.flightStorageManager = new FlightStorageManager(this);
        this.flightStorageManager.load();
        this.messageHelper = new MessageHelper(this);

        this.vanishService = new VanishService(this);
        this.autoUpdateService = new AutoUpdateService(this);
        this.logCleanupService = new LogCleanupService(this);
        this.vanishService.load();

        PluginCommand vanishCommand = getCommand("vanish");
        if (vanishCommand == null) {
            throw new IllegalStateException("Command 'vanish' was not defined in plugin.yml");
        }

        PluginCommand invisCommand = getCommand("invis");
        if (invisCommand == null) {
            throw new IllegalStateException("Command 'invis' was not defined in plugin.yml");
        }
        PluginCommand confirmChatCommand = getCommand("vanishchatconfirm");
        if (confirmChatCommand == null) {
            throw new IllegalStateException("Command 'vanishchatconfirm' was not defined in plugin.yml");
        }
        PluginCommand cleanLogsCommand = getCommand("cleanlogs");
        if (cleanLogsCommand == null) {
            throw new IllegalStateException("Command 'cleanlogs' was not defined in plugin.yml");
        }
        PluginCommand sudoCommand = getCommand("sudo");
        if (sudoCommand == null) {
            throw new IllegalStateException("Command 'sudo' was not defined in plugin.yml");
        }
        PluginCommand mayflyCommand = getCommand("mayfly");
        if (mayflyCommand == null) {
            throw new IllegalStateException("Command 'mayfly' was not defined in plugin.yml");
        }

        vanishCommand.setExecutor(new ToggleStateCommand(
            "/vanish",
            this.vanishService::toggleVanish,
            enabled -> enabled ? "Vanish enabled." : "Vanish disabled."
        ));
        invisCommand.setExecutor(new ToggleStateCommand(
            "/invis",
            this.vanishService::toggleInvis,
            enabled -> enabled ? "Invis enabled." : "Invis disabled."
        ));
        confirmChatCommand.setExecutor(new ConfirmChatCommand(this.vanishService));
        cleanLogsCommand.setExecutor(new CleanLogsCommand(this.logCleanupService));
        sudoCommand.setExecutor(new SudoCommand());
        sudoCommand.setTabCompleter(new SudoTabCompleter());
        mayflyCommand.setExecutor(new MayFlyCommand(this));
        mayflyCommand.setTabCompleter(new MayFlyTabCompleter());

        Bukkit.getPluginManager().registerEvents(new VanishListener(this, this.vanishService), this);
        Bukkit.getPluginManager().registerEvents(new SilentCommandListener(cleanLogsCommand), this);
        Bukkit.getPluginManager().registerEvents(new FlightJoinListener(this), this);
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            new ProtocolLibCleanLogsIntegration(this, cleanLogsCommand).register();
            getLogger().info("ProtocolLib detected. /cleanlogs console logging will be suppressed.");
        }

        this.vanishService.refreshConfiguredOfflineAliases();
        this.vanishService.reapplyToOnlinePlayers();
        this.autoUpdateService.checkForUpdatesOnStartup();
        getLogger().info("ODYAdminCommands enabled.");
    }

    @Override
    public void onDisable() {
        if (this.vanishService != null) {
            this.vanishService.save();
        }
        if (this.flightStorageManager != null) {
            this.flightStorageManager.save();
        }
    }

    public Set<String> loadOfflineCommandAliases() {
        FileConfiguration config = getConfig();
        return config.getStringList("offline-command-aliases").stream()
            .map(alias -> alias.toLowerCase(Locale.ROOT))
            .filter(alias -> !alias.isBlank())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public FlightStorageManager getStorageManager() {
        return this.flightStorageManager;
    }

    public MessageHelper getMessageHelper() {
        return this.messageHelper;
    }
}
