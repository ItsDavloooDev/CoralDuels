package dev.itsdavlooo.coralduels.domain.arena;

import org.bukkit.Location;
import org.bukkit.World;

public final class Arena {

    private final String name;
    private final String worldName;
    private final Location spawn1;
    private final Location spawn2;
    private final ArenaBounds bounds;
    private final boolean enabled;

    public Arena(String name, String worldName, Location spawn1, Location spawn2,
                 ArenaBounds bounds, boolean enabled) {
        this.name = name;
        this.worldName = worldName;
        this.spawn1 = spawn1;
        this.spawn2 = spawn2;
        this.bounds = bounds;
        this.enabled = enabled;
    }

    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public Location getSpawn1() { return spawn1; }
    public Location getSpawn2() { return spawn2; }
    public ArenaBounds getBounds() { return bounds; }
    public boolean isEnabled() { return enabled; }

    public World getWorld() {
        return org.bukkit.Bukkit.getWorld(worldName);
    }

    public boolean contains(Location location) {
        return bounds.contains(location);
    }

    public static class ArenaBounds {
        private final double minX, maxX, minY, maxY, minZ, maxZ;

        public ArenaBounds(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        public boolean contains(Location loc) {
            return loc.getX() >= minX && loc.getX() <= maxX
                    && loc.getY() >= minY && loc.getY() <= maxY
                    && loc.getZ() >= minZ && loc.getZ() <= maxZ;
        }
    }
}