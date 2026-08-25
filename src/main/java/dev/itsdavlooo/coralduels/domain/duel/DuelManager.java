package dev.itsdavlooo.coralduels.domain.duel;

import dev.itsdavlooo.coralduels.domain.arena.Arena;
import dev.itsdavlooo.coralduels.domain.arena.ArenaManager;
import dev.itsdavlooo.coralduels.domain.kit.Kit;
import dev.itsdavlooo.coralduels.domain.player.DuelPlayer;
import dev.itsdavlooo.coralduels.domain.player.PlayerStateManager;
import dev.itsdavlooo.coralduels.domain.reward.RewardManager;
import dev.itsdavlooo.coralduels.domain.statistic.StatisticManager;
import org.bukkit.Bukkit;
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
    private final ArenaManager arenaManager;

    public DuelManager(RequestManager requestManager, SessionManager sessionManager,
                       PlayerStateManager playerStateManager, RewardManager rewardManager,
                       StatisticManager statisticManager, ArenaManager arenaManager) {
        this.requestManager = requestManager;
        this.sessionManager = sessionManager;
        this.playerStateManager = playerStateManager;
        this.rewardManager = rewardManager;
        this.statisticManager = statisticManager;
        this.arenaManager = arenaManager;
    }

    public DuelRequest addRequest(UUID challenger, UUID target, Kit kit) {
        return requestManager.addRequest(challenger, target, kit);
    }

    public void startDuel(DuelRequest request) {
        Optional<Arena> arenaOpt = arenaManager.getAvailableArena();
        if (arenaOpt.isEmpty()) {
            Player challenger = Bukkit.getPlayer(request.challenger());
            Player target = Bukkit.getPlayer(request.target());
            if (challenger != null) challenger.sendMessage("§cNessuna arena disponibile.");
            if (target != null) target.sendMessage("§cNessuna arena disponibile.");
            return;
        }

        Arena arena = arenaOpt.get();
        Player challengerPlayer = Bukkit.getPlayer(request.challenger());
        Player targetPlayer = Bukkit.getPlayer(request.target());

        if (challengerPlayer == null || targetPlayer == null) {
            return;
        }

        DuelPlayer challenger = new DuelPlayer(challengerPlayer);
        DuelPlayer targetDuel = new DuelPlayer(targetPlayer);

        playerStateManager.saveState(challengerPlayer.getUniqueId(), challenger);
        playerStateManager.saveState(targetPlayer.getUniqueId(), targetDuel);

        DuelSession session = new DuelSession(
                UUID.randomUUID(),
                challenger,
                targetDuel,
                request.kit(),
                arena,
                DuelState.COUNTDOWN,
                Instant.now(),
                null
        );

        sessionManager.addSession(session);
        playerStateManager.registerDuel(new Duel(session));

        challengerPlayer.teleport(arena.getSpawn1());
        targetPlayer.teleport(arena.getSpawn2());

        request.kit().giveItems(challengerPlayer);
        request.kit().giveItems(targetPlayer);
    }

    public void cancelDuel(UUID playerUuid) {
        sessionManager.getSessionByPlayer(playerUuid).ifPresent(session -> {
            UUID opponentUuid = session.challenger().getUuid().equals(playerUuid)
                    ? session.target().getUuid()
                    : session.challenger().getUuid();

            Player player = Bukkit.getPlayer(playerUuid);
            Player opponent = Bukkit.getPlayer(opponentUuid);

            if (player != null) {
                playerStateManager.restoreSnapshot(playerUuid, player);
            }
            if (opponent != null) {
                playerStateManager.restoreSnapshot(opponentUuid, opponent);
            }

            sessionManager.removeSession(session.id());
            playerStateManager.unregisterDuel(new Duel(session));
        });
    }

    public void endDuel(UUID sessionId, UUID winner, int challengerDamage, int targetDamage) {
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