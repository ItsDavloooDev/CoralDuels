package dev.itsdavlooo.coralduels.listener;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.domain.player.PlayerStateManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public final class WorldListener implements Listener {

    private final PlayerStateManager playerStateManager;

    public WorldListener() {
        this.playerStateManager = CoralDuelsPlugin.getInstance().getPlayerStateManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (playerStateManager.isInDuel(event.getPlayer().getUniqueId())) {
            event.getPlayer().sendMessage("§cNon puoi cambiare mondo durante un duello.");
            event.getPlayer().teleport(event.getFrom());
        }
    }
}