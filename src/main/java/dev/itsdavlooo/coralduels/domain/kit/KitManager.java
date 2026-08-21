package dev.itsdavlooo.coralduels.domain.kit;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.service.config.ConfigService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class KitManager {

    private final Map<String, Kit> kits = new HashMap<>();
    private final ConfigService config;

    public KitManager(ConfigService config) {
        this.config = config;
        loadKits();
    }

    private void loadKits() {
        kits.clear();
        ConfigurationSection section = config.getKits().getConfigurationSection("kits");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection kitSection = section.getConfigurationSection(key);
                if (kitSection != null) {
                    Kit kit = Kit.fromConfig(key, kitSection);
                    kits.put(key.toLowerCase(), kit);
                }
            }
        }
    }

    public void reload() {
        loadKits();
    }

    public Optional<Kit> getKit(String name) {
        return Optional.ofNullable(kits.get(name.toLowerCase()));
    }

    public boolean hasKit(String name) {
        return kits.containsKey(name.toLowerCase());
    }

    public boolean canUse(Player player, Kit kit) {
        return player.hasPermission(kit.getPermission()) || player.hasPermission("coralduels.kit.select.*");
    }

    public void giveKit(Player player, Kit kit) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        for (Map.Entry<Integer, ItemStack> entry : kit.getItems().entrySet()) {
            player.getInventory().setItem(entry.getKey(), entry.getValue().clone());
        }

        ItemStack[] armor = kit.getArmor();
        if (armor.length >= 4) {
            player.getInventory().setHelmet(armor[3].clone());
            player.getInventory().setChestplate(armor[2].clone());
            player.getInventory().setLeggings(armor[1].clone());
            player.getInventory().setBoots(armor[0].clone());
        }

        for (KitEffect effect : kit.getEffects()) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    effect.type(), effect.duration(), effect.amplifier(), true, false));
        }

        player.updateInventory();
    }

    public Map<String, Kit> getAllKits() {
        return Map.copyOf(kits);
    }
}