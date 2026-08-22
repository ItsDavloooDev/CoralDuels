package dev.itsdavlooo.coralduels.domain.kit;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public record KitDefinition(
        String name,
        String displayName,
        String permission,
        ItemStack icon,
        Map<Integer, ItemStack> items,
        ItemStack[] armor,
        List<KitEffect> effects,
        int cooldown
) {
    public Kit toKit() {
        return new Kit(name, displayName, permission, icon, items, armor, effects, cooldown);
    }

    public static KitDefinition fromKit(Kit kit) {
        return new KitDefinition(
                kit.getName(),
                kit.getDisplayName(),
                kit.getPermission(),
                kit.getIcon(),
                kit.getItems(),
                kit.getArmor(),
                kit.getEffects(),
                kit.getCooldown()
        );
    }
}