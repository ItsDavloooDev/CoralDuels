package dev.itsdavlooo.coralduels;

import org.bukkit.plugin.java.JavaPlugin;

public final class CoralDuelsPlugin extends JavaPlugin {

    private static CoralDuelsPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    public static CoralDuelsPlugin getInstance() {
        return instance;
    }
}
