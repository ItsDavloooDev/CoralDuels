package dev.itsdavlooo.coralduels.listener;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.domain.duel.DuelManager;
import dev.itsdavlooo.coralduels.domain.duel.RequestManager;
import dev.itsdavlooo.coralduels.domain.player.PlayerSnapshot;
import dev.itsdavlooo.coralduels.domain.player.PlayerStateManager;
import dev.itsdavlooo.coralduels.service.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerListener implements Listener {

    private final DuelManager duelManager;
    private final RequestManager requestManager;
    private final PlayerStateManager playerStateManager;
    private final MessageService messages;
    private final Map<UUID, Location> pendingRespawns = new ConcurrentHashMap<>();

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
        if (playerStateManager.getSnapshot(player.getUniqueId()).isPresent()) {
            event.setDeathMessage(null);
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            playerStateManager.getSnapshot(player.getUniqueId())
                    .map(PlayerSnapshot::location)
                    .ifPresent(location -> pendingRespawns.put(player.getUniqueId(), location));
            if (playerStateManager.isInDuel(player.getUniqueId())) {
                duelManager.handleDuelDeath(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location respawnLocation = pendingRespawns.remove(player.getUniqueId());
        if (respawnLocation != null) {
            event.setRespawnLocation(respawnLocation);
            UUID uuid = player.getUniqueId();
            CoralDuelsPlugin plugin = CoralDuelsPlugin.getInstance();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Player online = Bukkit.getPlayer(uuid);
                if (online != null) {
                    playerStateManager.restoreSnapshot(uuid, online);
                }
            });
        }
    }

    private void handlePlayerLeave(Player player) {
        UUID uuid = player.getUniqueId();

        requestManager.removeRequestByTarget(uuid);
        requestManager.getRequest(uuid).ifPresent(req -> requestManager.removeRequest(req.id()));

        if (playerStateManager.isInDuel(uuid)) {
            duelManager.cancelDuel(uuid);
        }

        pendingRespawns.remove(uuid);
        playerStateManager.cleanupPlayer(uuid);
    }
}
