package com.odyadmincommands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class LogCleanupService {
    private final JavaPlugin plugin;

    public LogCleanupService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public CleanupResult clean() {
        Path serverRoot = resolveServerRoot();
        if (serverRoot == null) {
            return new CleanupResult(0, 0, true);
        }

        AtomicInteger deletedLogs = new AtomicInteger();
        AtomicInteger deletedCrashLogs = new AtomicInteger();
        boolean success = true;

        success &= cleanLogsDirectory(serverRoot.resolve("logs"), deletedLogs);
        success &= cleanDirectory(serverRoot.resolve("crash-reports"), deletedCrashLogs);

        return new CleanupResult(deletedLogs.get(), deletedCrashLogs.get(), success);
    }

    private Path resolveServerRoot() {
        Path pluginDataFolder = this.plugin.getDataFolder().toPath();
        Path pluginsDirectory = pluginDataFolder.getParent();
        if (pluginsDirectory == null) {
            return null;
        }

        Path serverRoot = pluginsDirectory.getParent();
        if (serverRoot != null) {
            return serverRoot;
        }

        return pluginsDirectory;
    }

    private boolean cleanLogsDirectory(Path logsDirectory, AtomicInteger deletedLogs) {
        if (!Files.isDirectory(logsDirectory)) {
            return true;
        }

        boolean success = true;
        try (var paths = Files.walk(logsDirectory)) {
            for (Path path : paths.sorted((left, right) -> right.getNameCount() - left.getNameCount()).toList()) {
                if (path.equals(logsDirectory)) {
                    continue;
                }

                if (Files.isDirectory(path)) {
                    Files.deleteIfExists(path);
                    continue;
                }

                if (path.getFileName().toString().equalsIgnoreCase("latest.log")) {
                    Files.write(path, new byte[0]);
                    deletedLogs.incrementAndGet();
                    continue;
                }

                Files.deleteIfExists(path);
                deletedLogs.incrementAndGet();
            }
        } catch (IOException exception) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to clean logs directory", exception);
            success = false;
        }

        return success;
    }

    private boolean cleanDirectory(Path directory, AtomicInteger deletedFiles) {
        if (!Files.isDirectory(directory)) {
            return true;
        }

        boolean success = true;
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted((left, right) -> right.getNameCount() - left.getNameCount()).toList()) {
                if (path.equals(directory)) {
                    continue;
                }

                Files.deleteIfExists(path);
                if (!Files.isDirectory(path)) {
                    deletedFiles.incrementAndGet();
                }
            }
        } catch (IOException exception) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to clean crash reports directory", exception);
            success = false;
        }

        return success;
    }

    public record CleanupResult(int deletedLogFiles, int deletedCrashLogs, boolean success) {
    }
}
