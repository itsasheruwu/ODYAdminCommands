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

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.vanishService = new VanishService(this);
        this.autoUpdateService = new AutoUpdateService(this);
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

        Bukkit.getPluginManager().registerEvents(new VanishListener(this, this.vanishService), this);

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
    }

    public Set<String> loadOfflineCommandAliases() {
        FileConfiguration config = getConfig();
        return config.getStringList("offline-command-aliases").stream()
            .map(alias -> alias.toLowerCase(Locale.ROOT))
            .filter(alias -> !alias.isBlank())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
