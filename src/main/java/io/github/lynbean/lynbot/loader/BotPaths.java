package io.github.lynbean.lynbot.loader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import lombok.Getter;

public enum BotPaths {
    COGSDIR("cogs"),
    CONFIGDIR("config");

    private @Getter final Path relativePath;
    private @Getter final boolean isDirectory;
    private Path absolutePath;

    BotPaths(String... path) {
        this.relativePath = computePath(path);
        this.isDirectory = true;
    }

    private Path computePath(String... path) {
        return Paths.get(path[0], Arrays.copyOfRange(path, 1, path.length));
    }

    public static void setup() {
        final Path rootPath = Paths.get("");
        loadAbsolutePaths(rootPath);
    }

    private static void loadAbsolutePaths(Path rootPath) {
        for (BotPaths path : BotPaths.values()) {
            path.absolutePath = rootPath.resolve(path.relativePath).toAbsolutePath().normalize();

            if (path.isDirectory && !Files.isDirectory(path.absolutePath)) {
                try {
                    Files.createDirectories(path.absolutePath);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create directory: " + path.absolutePath, e);
                }
            }
        }
    }
}
