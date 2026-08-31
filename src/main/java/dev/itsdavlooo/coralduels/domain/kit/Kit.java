package dev.itsdavlooo.coralduels.domain.kit;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Kit {

    private final String name;
    private final String displayName;
    private final String permission;
    private final ItemStack icon;
    private final Map<Integer, ItemStack> items;
    private final ItemStack[] armor;
    private final List<KitEffect> effects;
    private final int cooldown;

    public Kit(String name, String displayName, String permission, ItemStack icon,
                Map<Integer, ItemStack> items, ItemStack[] armor, List<KitEffect> effects, int cooldown) {
        this.name = name;
        this.displayName = displayName;
        this.permission = permission;
        this.icon = icon;
        this.items = items;
        this.armor = armor;
        this.effects = effects;
        this.cooldown = cooldown;
    }

    public static Kit fromConfig(String name, ConfigurationSection section) {
        String displayName = section.getString("display-name", name);
        String permission = section.getString("permission", "coralduels.kit.select." + name);
        ItemStack icon = parseItemStack(section.getConfigurationSection("icon"));
        Map<Integer, ItemStack> items = parseItems(section, "items");
        ItemStack[] armor = parseArmor(section.getConfigurationSection("armor"));
        List<KitEffect> effects = parseEffects(section.getList("effects"));
        int cooldown = section.getInt("cooldown", 0);
        return new Kit(name, displayName, permission, icon, items, armor, effects, cooldown);
    }

    private static ItemStack parseItemStack(ConfigurationSection section) {
        if (section == null) return new ItemStack(Material.BARRIER);
        Material material = Material.getMaterial(section.getString("material", "BARRIER"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = section.getString("name");
            if (name != null) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> lore = section.getStringList("lore");
            if (!lore.isEmpty()) {
                meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            }
            ConfigurationSection enchants = section.getConfigurationSection("enchantments");
            if (enchants != null) {
                for (String key : enchants.getKeys(false)) {
                    Enchantment enchant = Enchantment.getByName(key);
                    if (enchant != null) meta.addEnchant(enchant, enchants.getInt(key), true);
                }
            }
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static Map<Integer, ItemStack> parseItems(ConfigurationSection section, String path) {
        Map<Integer, ItemStack> items = new HashMap<>();
        if (section == null) return items;

        List<?> list = section.getList(path);
        if (list != null) {
            for (Object obj : list) {
                if (!(obj instanceof Map<?, ?> raw)) continue;
                Map<String, Object> map = new HashMap<>();
                raw.forEach((k, v) -> map.put(String.valueOf(k), v));
                int slot = ((Number) map.getOrDefault("slot", 0)).intValue();
                ItemStack item = parseItemFromMap(map);
                if (item != null) items.put(slot, item);
            }
            return items;
        }

        ConfigurationSection itemSection = section.getConfigurationSection(path);
        if (itemSection == null) return items;
        for (String key : itemSection.getKeys(false)) {
            ConfigurationSection entry = itemSection.getConfigurationSection(key);
            if (entry == null) continue;
            Map<String, Object> map = new HashMap<>();
            for (String entryKey : entry.getKeys(false)) {
                map.put(entryKey, entry.get(entryKey));
            }
            int slot = entry.getInt("slot", 0);
            ItemStack item = parseItemFromMap(map);
            if (item != null) items.put(slot, item);
        }
        return items;
    }

    private static ItemStack parseItemFromMap(Map<String, Object> map) {
        String materialName = String.valueOf(map.getOrDefault("material", "AIR"));
        Material material = Material.getMaterial(materialName);
        if (material == null) return null;
        int amount = ((Number) map.getOrDefault("amount", 1)).intValue();
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Object enchants = map.get("enchantments");
            if (enchants instanceof Map<?, ?> enchMap) {
                for (Map.Entry<?, ?> entry : enchMap.entrySet()) {
                    Enchantment enchant = Enchantment.getByName(String.valueOf(entry.getKey()));
                    if (enchant != null) meta.addEnchant(enchant, ((Number) entry.getValue()).intValue(), true);
                }
            }
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack[] parseArmor(ConfigurationSection section) {
        ItemStack[] armor = new ItemStack[4];
        if (section == null) return armor;
        String helmet = section.getString("helmet", "AIR");
        String chestplate = section.getString("chestplate", "AIR");
        String leggings = section.getString("leggings", "AIR");
        String boots = section.getString("boots", "AIR");
        armor[3] = new ItemStack(Material.getMaterial(helmet));
        armor[2] = new ItemStack(Material.getMaterial(chestplate));
        armor[1] = new ItemStack(Material.getMaterial(leggings));
        armor[0] = new ItemStack(Material.getMaterial(boots));
        return armor;
    }

    private static List<KitEffect> parseEffects(List<?> list) {
        if (list == null) return new ArrayList<>();
        List<KitEffect> effects = new ArrayList<>();
        for (Object obj : list) {
            if (obj instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) obj;
                String type = (String) map.get("type");
                int amplifier = ((Number) map.getOrDefault("amplifier", 0)).intValue();
                int duration = ((Number) map.getOrDefault("duration", 0)).intValue();
                PotionEffectType potionType = PotionEffectType.getByName(type);
                if (potionType != null) {
                    effects.add(new KitEffect(potionType, amplifier, duration));
                }
            }
        }
        return effects;
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getPermission() { return permission; }
    public ItemStack getIcon() { return icon; }
    public Map<Integer, ItemStack> getItems() { return items; }
    public ItemStack[] getArmor() { return armor; }
    public List<KitEffect> getEffects() { return effects; }
    public int getCooldown() { return cooldown; }

    public void giveItems(org.bukkit.entity.Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            player.getInventory().setItem(entry.getKey(), entry.getValue().clone());
        }

        if (armor.length >= 4) {
            player.getInventory().setHelmet(armor[3].clone());
            player.getInventory().setChestplate(armor[2].clone());
            player.getInventory().setLeggings(armor[1].clone());
            player.getInventory().setBoots(armor[0].clone());
        }

        for (KitEffect effect : effects) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    effect.type(), effect.duration(), effect.amplifier(), true, false));
        }

        player.updateInventory();
    }
}