package dev.itsdavlooo.coralduels.listener;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.domain.duel.Duel;
import dev.itsdavlooo.coralduels.domain.duel.DuelManager;
import dev.itsdavlooo.coralduels.domain.duel.DuelState;
import dev.itsdavlooo.coralduels.domain.player.PlayerStateManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class DuelListener implements Listener {

    private final DuelManager duelManager;
    private final PlayerStateManager playerStateManager;

    public DuelListener() {
        CoralDuelsPlugin plugin = CoralDuelsPlugin.getInstance();
        this.duelManager = plugin.getDuelManager();
        this.playerStateManager = plugin.getPlayerStateManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        if (!playerStateManager.isInDuel(victim.getUniqueId()) || !playerStateManager.isInDuel(attacker.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        var victimDuel = playerStateManager.getActiveDuel(victim.getUniqueId());
        var attackerDuel = playerStateManager.getActiveDuel(attacker.getUniqueId());
        if (victimDuel == null || attackerDuel == null || !victimDuel.getId().equals(attackerDuel.getId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageReceive(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!playerStateManager.isInDuel(player.getUniqueId())) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                || event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            Duel duel = playerStateManager.getActiveDuel(player.getUniqueId());
            if (duel != null && duel.getState() == DuelState.COUNTDOWN) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            duelManager.handleDuelDeath(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!playerStateManager.isInDuel(player.getUniqueId())) return;

        String cmd = event.getMessage().toLowerCase();
        if (cmd.startsWith("/kit") || cmd.startsWith("/spawn") || cmd.startsWith("/home") || cmd.startsWith("/tp") || cmd.startsWith("/tpa")) {
            event.setCancelled(true);
            player.sendMessage("§cNon puoi usare questo comando durante un duello.");
        }
    }
}