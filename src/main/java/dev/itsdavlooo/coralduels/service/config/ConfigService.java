package dev.itsdavlooo.coralduels.service.config;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public final class ConfigService {

    private final CoralDuelsPlugin plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, File> configFiles = new HashMap<>();

    public ConfigService(CoralDuelsPlugin plugin) {
        this.plugin = plugin;
        loadAll();
    }

    private void loadAll() {
        load("config.yml");
        load("messages.yml");
        load("kits.yml");
        load("rewards.yml");
        load("arenas.yml");
        load("leaderboard.yml");
    }

    public void load(String name) {
        File file = new File(plugin.getDataFolder(), name);
        configFiles.put(name, file);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        try (InputStream def = plugin.getResource(name)) {
            if (def != null) {
                config.setDefaults(YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(def)));
            }
        } catch (IOException ignored) {}
        configs.put(name, config);
    }

    public void reload() {
        configs.clear();
        loadAll();
    }

    public FileConfiguration get(String name) {
        return configs.get(name);
    }

    public FileConfiguration getConfig() { return get("config.yml"); }
    public FileConfiguration getMessages() { return get("messages.yml"); }
    public FileConfiguration getKits() { return get("kits.yml"); }
    public FileConfiguration getRewards() { return get("rewards.yml"); }
    public FileConfiguration getArenas() { return get("arenas.yml"); }
    public FileConfiguration getLeaderboard() { return get("leaderboard.yml"); }

    public void save(String name) {
        File file = configFiles.get(name);
        FileConfiguration config = configs.get(name);
        if (file != null && config != null) {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save " + name + ": " + e.getMessage());
            }
        }
    }
}