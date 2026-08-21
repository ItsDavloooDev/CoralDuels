package dev.itsdavlooo.coralduels.domain.kit;

import org.bukkit.potion.PotionEffectType;

public record KitEffect(PotionEffectType type, int amplifier, int duration) {}