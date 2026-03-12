package com.odyadmincommands;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;

public final class AutoUpdateService {
    private static final String GITHUB_OWNER = "itsasheruwu";
    private static final String GITHUB_REPO = "ODYAdminCommands";
    private static final String RELEASE_ASSET_NAME = "ODYAdminCommands.jar";
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ASSET_PATTERN = Pattern.compile(
        "\\{[^\\{]*?\"name\"\\s*:\\s*\"([^\"]+)\"[^\\{]*?\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"[^\\{]*?\\}",
        Pattern.DOTALL
    );

    private final ODYAdminCommandsPlugin plugin;
    private final HttpClient httpClient;

    public AutoUpdateService(ODYAdminCommandsPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public void checkForUpdatesOnStartup() {
        ConfigurationSection updater = this.plugin.getConfig().getConfigurationSection("auto-update");
        if (updater == null || !updater.getBoolean("enabled", true)) {
            return;
        }

        this.plugin.getServer().getAsyncScheduler().runNow(this.plugin, task -> {
            try {
                LatestRelease latestRelease = fetchLatestRelease();
                String currentVersion = this.plugin.getPluginMeta().getVersion();
                if (!isNewerVersion(normalizeVersion(latestRelease.tagName()), normalizeVersion(currentVersion))) {
                    this.plugin.getLogger().info("Auto-update check: already on the latest release (" + currentVersion + ").");
                    return;
                }

                Path updateFolder = this.plugin.getServer().getUpdateFolderFile().toPath();
                Files.createDirectories(updateFolder);

                Path targetFile = updateFolder.resolve(latestRelease.assetName());
                downloadReleaseAsset(latestRelease.downloadUrl(), targetFile);

                this.plugin.getLogger().info(
                    "Downloaded update " + latestRelease.tagName() + " to " + targetFile + ". It will be applied on the next restart."
                );
            } catch (Exception exception) {
                this.plugin.getLogger().log(Level.WARNING, "Auto-update check failed", exception);
            }
        });
    }

    private LatestRelease fetchLatestRelease() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases/latest"))
            .timeout(Duration.ofSeconds(15))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "ODYAdminCommands-Updater")
            .build();

        HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("GitHub API returned status " + response.statusCode());
        }

        String body = response.body();
        String tagName = extractRequired(body, TAG_NAME_PATTERN, "tag_name");
        Optional<ReleaseAsset> asset = findReleaseAsset(body);
        if (asset.isEmpty()) {
            throw new IOException("Release asset '" + RELEASE_ASSET_NAME + "' was not found in the latest release");
        }

        return new LatestRelease(tagName, asset.get().name(), asset.get().downloadUrl());
    }

    private Optional<ReleaseAsset> findReleaseAsset(String body) {
        Matcher matcher = ASSET_PATTERN.matcher(body);
        while (matcher.find()) {
            String assetName = unescapeJson(matcher.group(1));
            String downloadUrl = unescapeJson(matcher.group(2));
            if (assetName.equals(RELEASE_ASSET_NAME)) {
                return Optional.of(new ReleaseAsset(assetName, downloadUrl));
            }
        }

        return Optional.empty();
    }

    private void downloadReleaseAsset(String downloadUrl, Path targetFile) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(downloadUrl))
            .timeout(Duration.ofSeconds(60))
            .header("Accept", "application/octet-stream")
            .header("User-Agent", "ODYAdminCommands-Updater")
            .build();

        HttpResponse<InputStream> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("GitHub asset download returned status " + response.statusCode());
        }

        try (InputStream stream = response.body()) {
            Files.copy(stream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean isNewerVersion(String latest, String current) {
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");
        int maxLength = Math.max(latestParts.length, currentParts.length);

        for (int index = 0; index < maxLength; index++) {
            int latestPart = parseVersionPart(latestParts, index);
            int currentPart = parseVersionPart(currentParts, index);
            if (latestPart > currentPart) {
                return true;
            }
            if (latestPart < currentPart) {
                return false;
            }
        }

        return false;
    }

    private int parseVersionPart(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }

        String cleaned = parts[index].replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) {
            return 0;
        }

        return Integer.parseInt(cleaned);
    }

    private String normalizeVersion(String rawVersion) {
        return rawVersion.startsWith("v") || rawVersion.startsWith("V") ? rawVersion.substring(1) : rawVersion;
    }

    private String extractRequired(String body, Pattern pattern, String fieldName) throws IOException {
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            throw new IOException("GitHub response did not contain " + fieldName);
        }
        return unescapeJson(matcher.group(1));
    }

    private String unescapeJson(String value) {
        return value
            .replace("\\/", "/")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }

    private record ReleaseAsset(String name, String downloadUrl) {
    }

    private record LatestRelease(String tagName, String assetName, String downloadUrl) {
    }
}
