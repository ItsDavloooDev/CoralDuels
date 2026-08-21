package dev.itsdavlooo.coralduels.service.world;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.service.config.ConfigService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public final class DuelWorldManager {

    private final CoralDuelsPlugin plugin;
    private final ConfigService config;
    private World duelWorld;

    public DuelWorldManager(CoralDuelsPlugin plugin, ConfigService config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void initialize() {
        String worldName = config.getConfig().getString("world.name", "duel_world");
        String templateName = config.getConfig().getString("world.template", "duel_template");
        boolean autoCreate = config.getConfig().getBoolean("world.auto-create", true);

        if (autoCreate) {
            loadOrCreateWorld(worldName, templateName);
        } else {
            duelWorld = Bukkit.getWorld(worldName);
        }
    }

    private void loadOrCreateWorld(String worldName, String templateName) {
        duelWorld = Bukkit.getWorld(worldName);
        if (duelWorld == null) {
            File templateDir = new File(plugin.getDataFolder(), "templates/" + templateName);
            if (templateDir.exists()) {
                copyTemplate(templateDir, worldName);
            } else {
                WorldCreator creator = new WorldCreator(worldName);
                creator.type(WorldType.FLAT);
                creator.generator("void");
                duelWorld = creator.createWorld();
            }
        }
        if (duelWorld != null && config.getConfig().getBoolean("world.keep-loaded", true)) {
            duelWorld.setAutoSave(false);
        }
    }

    private void copyTemplate(File template, String worldName) {
        File targetDir = new File(Bukkit.getWorldContainer(), worldName);
        try {
            if (targetDir.exists()) {
                deleteDirectory(targetDir.toPath());
            }
            Files.walkFileTree(template.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path target = targetDir.toPath().resolve(template.toPath().relativize(dir));
                    Files.createDirectories(target);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path target = targetDir.toPath().resolve(template.toPath().relativize(file));
                    Files.copy(file, target);
                    return FileVisitResult.CONTINUE;
                }
            });
            WorldCreator creator = new WorldCreator(worldName);
            duelWorld = creator.createWorld();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to copy world template: " + e.getMessage());
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public World getDuelWorld() {
        return duelWorld;
    }

    public void cleanup() {
        if (duelWorld != null) {
            Bukkit.unloadWorld(duelWorld, false);
        }
    }
}