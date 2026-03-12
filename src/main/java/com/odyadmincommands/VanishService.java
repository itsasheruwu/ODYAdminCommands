package com.odyadmincommands;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Locale;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class VanishService {
    public static final String VANISH_PERMISSION = "odyadmincommands.vanish";
    public static final String INVIS_PERMISSION = "odyadmincommands.invis";
    public static final String BYPASS_PERMISSION = "odyadmincommands.vanish.bypass";

    private final ODYAdminCommandsPlugin plugin;
    private final File vanishedDataFile;
    private final File invisDataFile;
    private final Set<UUID> vanishedPlayers = new HashSet<>();
    private final Set<UUID> invisiblePlayers = new HashSet<>();
    private final Map<UUID, PendingChatConfirmation> pendingChatConfirmations = new HashMap<>();
    private final Map<UUID, String> approvedChatMessages = new HashMap<>();
    private Set<String> offlineCommandAliases = Set.of("msg", "tell", "whisper", "w", "pm");

    public VanishService(ODYAdminCommandsPlugin plugin) {
        this.plugin = plugin;
        this.vanishedDataFile = new File(plugin.getDataFolder(), "vanished.yml");
        this.invisDataFile = new File(plugin.getDataFolder(), "invis.yml");
    }

    public void load() {
        loadUuidSet(this.vanishedDataFile, "vanished", this.vanishedPlayers);
        loadUuidSet(this.invisDataFile, "invisible", this.invisiblePlayers);
    }

    public void save() {
        try {
            if (!this.plugin.getDataFolder().exists() && !this.plugin.getDataFolder().mkdirs()) {
                throw new IOException("Could not create plugin data folder");
            }

            saveUuidSet(this.vanishedDataFile, "vanished", this.vanishedPlayers);
            saveUuidSet(this.invisDataFile, "invisible", this.invisiblePlayers);
        } catch (IOException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to save admin visibility state", exception);
        }
    }

    public void refreshConfiguredOfflineAliases() {
        this.offlineCommandAliases = this.plugin.loadOfflineCommandAliases();
        if (this.offlineCommandAliases.isEmpty()) {
            this.offlineCommandAliases = Set.of("msg", "tell", "whisper", "w", "pm");
        }
    }

    public boolean toggleVanish(Player player) {
        boolean vanished = !isVanished(player.getUniqueId());
        setVanished(player, vanished);
        return vanished;
    }

    public boolean toggleInvis(Player player) {
        boolean invisible = !isInvisible(player.getUniqueId());
        setInvisible(player, invisible);
        return invisible;
    }

    public void setVanished(Player player, boolean vanished) {
        if (vanished) {
            this.vanishedPlayers.add(player.getUniqueId());
        } else {
            this.vanishedPlayers.remove(player.getUniqueId());
            this.pendingChatConfirmations.remove(player.getUniqueId());
            this.approvedChatMessages.remove(player.getUniqueId());
        }

        updateAppearance(player);
        updateVisibility(player);
        save();
    }

    public void setInvisible(Player player, boolean invisible) {
        if (invisible) {
            this.invisiblePlayers.add(player.getUniqueId());
        } else {
            this.invisiblePlayers.remove(player.getUniqueId());
        }

        updateVisibility(player);
        save();
    }

    public boolean isVanished(Player player) {
        return isVanished(player.getUniqueId());
    }

    public boolean isVanished(UUID playerId) {
        return this.vanishedPlayers.contains(playerId);
    }

    public boolean isInvisible(Player player) {
        return isInvisible(player.getUniqueId());
    }

    public boolean isInvisible(UUID playerId) {
        return this.invisiblePlayers.contains(playerId);
    }

    public boolean canBypass(Player player) {
        return player.hasPermission(BYPASS_PERMISSION);
    }

    public boolean shouldAppearOfflineTo(Player viewer, Player target) {
        return isVanished(target) && !canBypass(viewer);
    }

    public boolean isOfflineCommandAlias(String alias) {
        return this.offlineCommandAliases.contains(alias.toLowerCase(Locale.ROOT));
    }

    public void reapplyToOnlinePlayers() {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        for (Player player : onlinePlayers) {
            updateAppearance(player);
            if (isVanished(player) || isInvisible(player)) {
                updateVisibility(player);
            }
        }

        for (Player viewer : onlinePlayers) {
            handleViewerJoin(viewer);
        }
    }

    public void handleViewerJoin(Player viewer) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) {
                continue;
            }
            applyVisibilityForViewer(viewer, target);
        }
    }

    public void updateVisibility(Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) {
                continue;
            }
            applyVisibilityForViewer(viewer, target);
        }
    }

    public void updateAppearance(Player player) {
        player.playerListName(Component.text(player.getName()));
    }

    public Component offlineTargetMessage(String input) {
        return Component.text("Player '" + input + "' is not online.");
    }

    public Component vanishedEnabledMessage() {
        return Component.text("Vanish enabled.");
    }

    public Component vanishedDisabledMessage() {
        return Component.text("Vanish disabled.");
    }

    public Component chatBlockedMessage(Player player, String message) {
        String token = UUID.randomUUID().toString();
        this.pendingChatConfirmations.put(player.getUniqueId(), new PendingChatConfirmation(token, message));

        return Component.text()
            .append(Component.text("You are in vanish. Are you sure you want to send this? ", NamedTextColor.RED))
            .append(Component.text("Send anyway", NamedTextColor.BLUE, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.runCommand("/vanishchatconfirm " + token)))
            .build();
    }

    public boolean confirmPendingChat(Player player, String token) {
        PendingChatConfirmation confirmation = this.pendingChatConfirmations.get(player.getUniqueId());
        if (confirmation == null || !confirmation.token().equals(token)) {
            return false;
        }

        this.pendingChatConfirmations.remove(player.getUniqueId());
        this.approvedChatMessages.put(player.getUniqueId(), confirmation.message());
        Bukkit.getScheduler().runTask(this.plugin, () -> player.chat(confirmation.message()));
        return true;
    }

    public boolean consumeApprovedChat(Player player, String message) {
        String approved = this.approvedChatMessages.get(player.getUniqueId());
        if (approved == null || !approved.equals(message)) {
            return false;
        }

        this.approvedChatMessages.remove(player.getUniqueId());
        return true;
    }

    private void applyVisibilityForViewer(Player viewer, Player target) {
        boolean bypass = canBypass(viewer);

        if (isInvisible(target) && !bypass) {
            viewer.hidePlayer(this.plugin, target);
        } else {
            viewer.showPlayer(this.plugin, target);
        }

        if (isVanished(target) && !bypass) {
            viewer.unlistPlayer(target);
            return;
        }

        if (!viewer.isListed(target)) {
            viewer.listPlayer(target);
        }
    }

    private void loadUuidSet(File file, String path, Set<UUID> target) {
        if (!file.exists()) {
            return;
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        for (String rawUuid : configuration.getStringList(path)) {
            try {
                target.add(UUID.fromString(rawUuid));
            } catch (IllegalArgumentException exception) {
                this.plugin.getLogger().log(Level.WARNING, "Ignoring invalid UUID in " + file.getName() + ": " + rawUuid, exception);
            }
        }
    }

    private void saveUuidSet(File file, String path, Set<UUID> values) throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(path, values.stream().map(UUID::toString).sorted().toList());
        configuration.save(file);
    }

    private record PendingChatConfirmation(String token, String message) {
    }
}
