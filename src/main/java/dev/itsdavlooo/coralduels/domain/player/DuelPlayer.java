package dev.itsdavlooo.coralduels.domain.player;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.UUID;

public final class DuelPlayer {

    private final UUID uuid;
    private final String name;
    private ItemStack[] inventory;
    private ItemStack[] armor;
    private ItemStack[] enderChest;
    private Location location;
    private double health;
    private int foodLevel;
    private float saturation;
    private GameMode gameMode;
    private boolean flying;
    private boolean allowFlight;
    private Collection<PotionEffect> effects;
    private int level;
    private float exp;

    public DuelPlayer(Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        saveState(player);
    }

    public void saveState(Player player) {
        this.inventory = player.getInventory().getContents().clone();
        this.armor = player.getInventory().getArmorContents().clone();
        this.enderChest = player.getEnderChest().getContents().clone();
        this.location = player.getLocation().clone();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.gameMode = player.getGameMode();
        this.flying = player.isFlying();
        this.allowFlight = player.getAllowFlight();
        this.effects = player.getActivePotionEffects();
        this.level = player.getLevel();
        this.exp = player.getExp();
    }

    public void restoreState(Player player) {
        player.getInventory().setContents(inventory);
        player.getInventory().setArmorContents(armor);
        player.getEnderChest().setContents(enderChest);
        player.teleport(location);
        player.setHealth(health);
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setGameMode(gameMode);
        player.setFlying(flying);
        player.setAllowFlight(allowFlight);
        player.getActivePotionEffects().forEach(player::removePotionEffect);
        effects.forEach(player::addPotionEffect);
        player.setLevel(level);
        player.setExp(exp);
        player.updateInventory();
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
}