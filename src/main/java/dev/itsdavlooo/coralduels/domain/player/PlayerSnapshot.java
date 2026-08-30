package dev.itsdavlooo.coralduels.domain.player;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.UUID;

public record PlayerSnapshot(
        UUID uuid,
        String name,
        ItemStack[] inventory,
        ItemStack[] armor,
        ItemStack[] enderChest,
        Location location,
        double health,
        int foodLevel,
        float saturation,
        GameMode gameMode,
        boolean flying,
        boolean allowFlight,
        Collection<PotionEffect> effects,
        int level,
        float exp
) {
    public static PlayerSnapshot fromPlayer(org.bukkit.entity.Player player) {
        return new PlayerSnapshot(
                player.getUniqueId(),
                player.getName(),
                player.getInventory().getContents().clone(),
                player.getInventory().getArmorContents().clone(),
                player.getEnderChest().getContents().clone(),
                player.getLocation().clone(),
                player.getHealth(),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getGameMode(),
                player.isFlying(),
                player.getAllowFlight(),
                player.getActivePotionEffects(),
                player.getLevel(),
                player.getExp()
        );
    }

    public void applyTo(org.bukkit.entity.Player player) {
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
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        effects.forEach(player::addPotionEffect);
        player.setLevel(level);
        player.setExp(exp);
        player.updateInventory();
    }
}