package dev.itsdavlooo.coralduels.listener;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.domain.duel.DuelManager;
import dev.itsdavlooo.coralduels.domain.duel.RequestManager;
import dev.itsdavlooo.coralduels.domain.player.PlayerStateManager;
import dev.itsdavlooo.coralduels.service.message.MessageService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class PlayerListener implements Listener {

    private final DuelManager duelManager;
    private final RequestManager requestManager;
    private final PlayerStateManager playerStateManager;
    private final MessageService messages;

    public PlayerListener() {
        CoralDuelsPlugin plugin = CoralDuelsPlugin.getInstance();
        this.duelManager = plugin.getDuelManager();
        this.requestManager = plugin.getRequestManager();
        this.playerStateManager = plugin.getPlayerStateManager();
        this.messages = plugin.getMessageService();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        handlePlayerLeave(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        handlePlayerLeave(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (playerStateManager.isInDuel(player.getUniqueId())) {
            event.setDeathMessage(null);
            duelManager.handleDuelDeath(player);
        }
    }

    private void handlePlayerLeave(Player player) {
        UUID uuid = player.getUniqueId();

        requestManager.removeRequestByTarget(uuid);
        requestManager.getRequest(uuid).ifPresent(req -> requestManager.removeRequest(req.id()));

        if (playerStateManager.isInDuel(uuid)) {
            duelManager.cancelDuel(uuid);
        }

        playerStateManager.cleanupPlayer(uuid);
    }
}