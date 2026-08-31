package dev.itsdavlooo.coralduels.listener;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.domain.player.PlayerStateManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class WorldListener implements Listener {

    private final PlayerStateManager playerStateManager;

    public WorldListener() {
        this.playerStateManager = CoralDuelsPlugin.getInstance().getPlayerStateManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (event.getFrom().getWorld() == event.getTo().getWorld()) {
            return;
        }
        if (!playerStateManager.isInDuel(player.getUniqueId())) {
            return;
        }
        if (playerStateManager.isDuelTeleporting(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage("§cNon puoi cambiare mondo durante un duello.");
    }
}
