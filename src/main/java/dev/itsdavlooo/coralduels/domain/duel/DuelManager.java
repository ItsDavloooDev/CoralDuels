package dev.itsdavlooo.coralduels.domain.duel;

import dev.itsdavlooo.coralduels.domain.arena.Arena;
import dev.itsdavlooo.coralduels.domain.kit.Kit;
import dev.itsdavlooo.coralduels.domain.player.DuelPlayer;
import dev.itsdavlooo.coralduels.domain.player.PlayerStateManager;
import dev.itsdavlooo.coralduels.domain.reward.RewardManager;
import dev.itsdavlooo.coralduels.domain.statistic.StatisticManager;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class DuelManager {

    private final RequestManager requestManager;
    private final SessionManager sessionManager;
    private final PlayerStateManager playerStateManager;
    private final RewardManager rewardManager;
    private final StatisticManager statisticManager;

    public DuelManager(RequestManager requestManager, SessionManager sessionManager,
                       PlayerStateManager playerStateManager, RewardManager rewardManager,
                       StatisticManager statisticManager) {
        this.requestManager = requestManager;
        this.sessionManager = sessionManager;
        this.playerStateManager = playerStateManager;
        this.rewardManager = rewardManager;
        this.statisticManager = statisticManager;
    }

    public Optional<DuelRequest> createRequest(UUID challenger, UUID target, Kit kit) {
        DuelRequest request = new DuelRequest(UUID.randomUUID(), challenger, target, kit, System.currentTimeMillis());
        requestManager.addRequest(request);
        return Optional.of(request);
    }

    public Optional<DuelSession> startDuel(DuelRequest request, Arena arena) {
        return Optional.empty();
    }

    public void endDuel(UUID sessionId, UUID winner, int challengerDamage, int targetDamage) {
    }

    public void cancelDuel(UUID sessionId) {
    }

    public void expireRequest(UUID requestId) {
    }

    public void applyRewards(Player winner, Player loser, String kitName) {
    }

    public void recordStatistics(UUID winner, UUID loser, String kitName, int challengerDamage, int targetDamage) {
    }

    public RequestManager getRequestManager() { return requestManager; }
    public SessionManager getSessionManager() { return sessionManager; }
    public PlayerStateManager getPlayerStateManager() { return playerStateManager; }
    public RewardManager getRewardManager() { return rewardManager; }
    public StatisticManager getStatisticManager() { return statisticManager; }
}