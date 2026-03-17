package com.odyadmincommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public final class MessageHelper {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final ODYAdminCommandsPlugin plugin;

    public MessageHelper(ODYAdminCommandsPlugin plugin) {
        this.plugin = plugin;
    }

    public Component getMessage(String key, String player, String executor) {
        FileConfiguration config = plugin.getConfig();
        String path = "messages." + key;
        String msg = config.getString(path, "&cMissing message: " + key);
        if (msg != null) {
            msg = msg.replace("%player%", player != null ? player : "");
            msg = msg.replace("%executor%", executor != null ? executor : "Console");
        }
        return colorize(msg);
    }

    public Component getMessage(String key, String player) {
        return getMessage(key, player, null);
    }

    public Component getMessage(String key) {
        return getMessage(key, null, null);
    }

    public void send(CommandSender to, String key, String player, String executor) {
        to.sendMessage(getMessage(key, player, executor));
    }

    public void send(CommandSender to, String key, String player) {
        send(to, key, player, null);
    }

    public void send(CommandSender to, String key) {
        send(to, key, null, null);
    }

    private static Component colorize(String text) {
        return LEGACY_SERIALIZER.deserialize(text == null ? "" : text);
    }
}
