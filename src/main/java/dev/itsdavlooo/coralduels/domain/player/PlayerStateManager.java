package dev.itsdavlooo.coralduels.domain.player;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.domain.duel.Duel;
import dev.itsdavlooo.coralduels.domain.duel.DuelState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerStateManager {

    private final Map<UUID, DuelPlayer> savedStates = new ConcurrentHashMap<>();
    private final Map<UUID, Duel> activeDuels = new ConcurrentHashMap<>();
    private final CoralDuelsPlugin plugin;

    public PlayerStateManager(CoralDuelsPlugin plugin) {
        this.plugin = plugin;
    }

    public void saveState(UUID uuid, DuelPlayer duelPlayer) {
        savedStates.put(uuid, duelPlayer);
    }

    public DuelPlayer getSavedState(UUID uuid) {
        return savedStates.get(uuid);
    }

    public boolean hasSavedState(UUID uuid) {
        return savedStates.containsKey(uuid);
    }

    public void removeSavedState(UUID uuid) {
        savedStates.remove(uuid);
    }

    public void registerDuel(Duel duel) {
        activeDuels.put(duel.getChallenger().getUuid(), duel);
        activeDuels.put(duel.getTarget().getUuid(), duel);
    }

    public void unregisterDuel(Duel duel) {
        activeDuels.remove(duel.getChallenger().getUuid());
        activeDuels.remove(duel.getTarget().getUuid());
    }

    public Duel getActiveDuel(UUID uuid) {
        return activeDuels.get(uuid);
    }

    public boolean isInDuel(UUID uuid) {
        Duel duel = activeDuels.get(uuid);
        return duel != null && duel.isActive();
    }

    public void cleanupPlayer(UUID uuid) {
        activeDuels.remove(uuid);
        savedStates.remove(uuid);
    }
}