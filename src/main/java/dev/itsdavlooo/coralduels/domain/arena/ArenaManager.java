package dev.itsdavlooo.coralduels.domain.arena;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.service.config.ConfigService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ArenaManager {

    private final Map<String, Arena> arenas = new HashMap<>();
    private final ConfigService config;
    private final CoralDuelsPlugin plugin;

    public ArenaManager(ConfigService config, CoralDuelsPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
        loadArenas();
    }

    private void loadArenas() {
        arenas.clear();
        ConfigurationSection section = config.getArenas().getConfigurationSection("arenas");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection arenaSection = section.getConfigurationSection(key);
                if (arenaSection != null) {
                    loadArena(key, arenaSection);
                }
            }
        }
    }

    private void loadArena(String key, ConfigurationSection section) {
        String name = section.getString("name", key);
        String worldName = section.getString("world", "duel_world");
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World " + worldName + " not found for arena " + key);
            return;
        }

        Location spawn1 = parseLocation(section.getConfigurationSection("spawn1"), world);
        Location spawn2 = parseLocation(section.getConfigurationSection("spawn2"), world);
        Arena.ArenaBounds bounds = parseBounds(section.getConfigurationSection("bounds"));
        boolean enabled = section.getBoolean("enabled", true);

        arenas.put(key, new Arena(name, worldName, spawn1, spawn2, bounds, enabled));
    }

    private Location parseLocation(ConfigurationSection section, World world) {
        if (section == null) return new Location(world, 0, 64, 0);
        return new Location(world,
                section.getDouble("x", 0),
                section.getDouble("y", 64),
                section.getDouble("z", 0),
                (float) section.getDouble("yaw", 0),
                (float) section.getDouble("pitch", 0));
    }

    private Arena.ArenaBounds parseBounds(ConfigurationSection section) {
        if (section == null) return new Arena.ArenaBounds(-100, 100, 0, 256, -100, 100);
        return new Arena.ArenaBounds(
                section.getDouble("min-x", -100),
                section.getDouble("max-x", 100),
                section.getDouble("min-y", 0),
                section.getDouble("max-y", 256),
                section.getDouble("min-z", -100),
                section.getDouble("max-z", 100)
        );
    }

    public Optional<Arena> getArena(String name) {
        return Optional.ofNullable(arenas.get(name));
    }

    public Optional<Arena> getAvailableArena() {
        return arenas.values().stream()
                .filter(Arena::isEnabled)
                .findFirst();
    }

    public Map<String, Arena> getAllArenas() {
        return Map.copyOf(arenas);
    }

    public void reload() {
        loadArenas();
    }
}