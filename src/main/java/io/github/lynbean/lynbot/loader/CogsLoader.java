package io.github.lynbean.lynbot.loader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.lynbean.lynbot.common.Cog;

public class CogsLoader {
    private static final BotPaths paths = BotPaths.COGSDIR;
    private static final List<Class<?>> availableCogs = new ArrayList<>();
    private static final Logger LOG = LoggerFactory.getLogger(CogsLoader.class);

    static {
        File cogsDir = paths.getRelativePath().toFile();
        File[] cogs = cogsDir.listFiles((dir, name) -> name.endsWith(".jar"));

        for (File cog : cogs) {
            try (
                URLClassLoader cl = new URLClassLoader(new URL[] { cog.toURI().toURL() });
                JarFile jar = new JarFile(cog);
            ) {
                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().endsWith(".class")) {
                        String className = entry.getName().replace("/", ".").substring(0, entry.getName().length() - 6);
                        Class<?> clazz = cl.loadClass(className);
                        if (clazz.isAnnotationPresent(Cog.class)) {
                            LOG.info("Found cog: {}", clazz.getName());
                            availableCogs.add(clazz);
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                LOG.error("Error while loading cogs", e);
            }
        }
    }

    public static List<Class<?>> getCogs() {
        return availableCogs;
    }
}
