package com.odyadmincommands;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FlightStorageManager {

    private final ODYAdminCommandsPlugin plugin;
    private final File dataFile;
    private final Map<UUID, Boolean> flightState = new HashMap<>();

    public FlightStorageManager(ODYAdminCommandsPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "flight-data.yml");
    }

    public void load() {
        flightState.clear();
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : configuration.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                flightState.put(uuid, configuration.getBoolean(key));
            } catch (IllegalArgumentException ignored) {
                // skip invalid UUIDs
            }
        }
    }

    public void save() {
        YamlConfiguration configuration = new YamlConfiguration();
        for (Map.Entry<UUID, Boolean> entry : flightState.entrySet()) {
            configuration.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            if (!this.plugin.getDataFolder().exists() && !this.plugin.getDataFolder().mkdirs()) {
                throw new IOException("Could not create plugin data folder");
            }
            configuration.save(dataFile);
        } catch (IOException exception) {
            this.plugin.getLogger().severe("Could not save flight data: " + exception.getMessage());
        }
    }

    public boolean isFlightEnabled(UUID uuid) {
        return Boolean.TRUE.equals(flightState.get(uuid));
    }

    public void setFlightEnabled(UUID uuid, boolean enabled) {
        flightState.put(uuid, enabled);
        save();
    }
}
