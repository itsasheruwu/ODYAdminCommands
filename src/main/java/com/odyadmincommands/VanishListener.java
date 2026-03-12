package com.odyadmincommands;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Arrays;
import java.util.Locale;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class VanishListener implements Listener {
    private final ODYAdminCommandsPlugin plugin;
    private final VanishService vanishService;

    public VanishListener(ODYAdminCommandsPlugin plugin, VanishService vanishService) {
        this.plugin = plugin;
        this.vanishService = vanishService;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.vanishService.updateAppearance(player);
        if (this.vanishService.isVanished(player)) {
            event.joinMessage(null);
        }

        Bukkit.getScheduler().runTask(this.plugin, () -> {
            this.vanishService.handleViewerJoin(player);
            if (this.vanishService.isVanished(player) || this.vanishService.isInvisible(player)) {
                this.vanishService.updateVisibility(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (this.vanishService.isVanished(event.getPlayer())) {
            event.quitMessage(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!this.vanishService.isVanished(player)) {
            return;
        }

        String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
        if (this.vanishService.consumeApprovedChat(player, plainMessage)) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(this.vanishService.chatBlockedMessage(player, plainMessage));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player sender = event.getPlayer();
        if (this.vanishService.canBypass(sender)) {
            return;
        }

        String message = event.getMessage();
        if (message.length() <= 1 || message.charAt(0) != '/') {
            return;
        }

        String[] parts = Arrays.stream(message.substring(1).split("\\s+"))
            .filter(part -> !part.isBlank())
            .toArray(String[]::new);
        if (parts.length < 2) {
            return;
        }

        String alias = normalizeAlias(parts[0]);
        if (!this.vanishService.isOfflineCommandAlias(alias)) {
            return;
        }

        String requestedTarget = parts[1];
        Player target = Bukkit.getPlayerExact(requestedTarget);
        if (target == null || !this.vanishService.shouldAppearOfflineTo(sender, target)) {
            return;
        }

        event.setCancelled(true);
        sender.sendMessage(this.vanishService.offlineTargetMessage(requestedTarget));
    }

    private String normalizeAlias(String rawAlias) {
        int namespaceSeparator = rawAlias.indexOf(':');
        String alias = namespaceSeparator >= 0 ? rawAlias.substring(namespaceSeparator + 1) : rawAlias;
        return alias.toLowerCase(Locale.ROOT);
    }
}
