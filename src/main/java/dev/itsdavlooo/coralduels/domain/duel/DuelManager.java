package dev.itsdavlooo.coralduels.domain.duel;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.domain.arena.Arena;
import dev.itsdavlooo.coralduels.domain.arena.ArenaManager;
import dev.itsdavlooo.coralduels.domain.kit.Kit;
import dev.itsdavlooo.coralduels.domain.player.DuelPlayer;
import dev.itsdavlooo.coralduels.domain.player.PlayerStateManager;
import dev.itsdavlooo.coralduels.domain.reward.RewardManager;
import dev.itsdavlooo.coralduels.domain.statistic.StatisticManager;
import dev.itsdavlooo.coralduels.service.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

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
    private final MessageService messages;
    private final int requestTimeoutSeconds;
    private BukkitTask cleanupTask;

    public DuelManager(RequestManager requestManager, SessionManager sessionManager,
                       PlayerStateManager playerStateManager, RewardManager rewardManager,
                       StatisticManager statisticManager, ArenaManager arenaManager) {
        this.requestManager = requestManager;
        this.sessionManager = sessionManager;
        this.playerStateManager = playerStateManager;
        this.rewardManager = rewardManager;
        this.statisticManager = statisticManager;
        this.arenaManager = arenaManager;
        this.messages = CoralDuelsPlugin.getInstance().getMessageService();
        this.requestTimeoutSeconds = CoralDuelsPlugin.getInstance().getConfigService().getConfig().getInt("timers.request-timeout", 30);
        startCleanupTask();
    }

    private void startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimer(CoralDuelsPlugin.getInstance(), () -> {
            long now = System.currentTimeMillis();
            requestManager.getPendingRequests().forEach(request -> {
                if (request.isExpired(requestTimeoutSeconds)) {
                    expireRequest(request.id());
                }
            });
        }, 20L, 20L);
    }

    public void stopCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
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

        startCountdown(session);
    }

    private void startCountdown(DuelSession session) {
        int countdown = CoralDuelsPlugin.getInstance().getConfigService().getConfig().getInt("timers.countdown", 3);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(CoralDuelsPlugin.getInstance(), new java.util.function.IntConsumer() {
            int remaining = countdown;
            @Override
            public void accept(int value) {
                if (remaining <= 0) {
                    beginDuel(session.id());
                    return;
                }
                Player challenger = Bukkit.getPlayer(session.challenger().getUuid());
                Player target = Bukkit.getPlayer(session.target().getUuid());
                if (challenger != null) messages.send(challenger, "duel-starting", java.util.Map.of("seconds", String.valueOf(remaining)));
                if (target != null) messages.send(target, "duel-starting", java.util.Map.of("seconds", String.valueOf(remaining)));
                remaining--;
            }
        }, 0L, 20L);
        sessionManager.updateSessionState(session.id(), DuelState.COUNTDOWN);
    }

    private void beginDuel(UUID sessionId) {
        sessionManager.getSession(sessionId).ifPresent(session -> {
            sessionManager.updateSessionState(sessionId, DuelState.ACTIVE);
            sessionManager.setStartedAt(sessionId, Instant.now());

            Player challenger = Bukkit.getPlayer(session.challenger().getUuid());
            Player target = Bukkit.getPlayer(session.target().getUuid());
            if (challenger != null) messages.send(challenger, "duel-start");
            if (target != null) messages.send(target, "duel-start");

            int maxDuration = CoralDuelsPlugin.getInstance().getConfigService().getConfig().getInt("timers.max-duel-duration", 300);
            Bukkit.getScheduler().runTaskLater(CoralDuelsPlugin.getInstance(), () -> {
                sessionManager.getSession(sessionId).ifPresent(s -> {
                    if (s.state() == DuelState.ACTIVE) {
                        handleDuelTimeout(sessionId);
                    }
                });
            }, maxDuration * 20L);
        });
    }

    private void handleDuelTimeout(UUID sessionId) {
        sessionManager.getSession(sessionId).ifPresent(session -> {
            Player challenger = Bukkit.getPlayer(session.challenger().getUuid());
            Player target = Bukkit.getPlayer(session.target().getUuid());
            if (challenger != null) messages.send(challenger, "duel-end-draw", java.util.Map.of("opponent", target != null ? target.getName() : "Unknown"));
            if (target != null) messages.send(target, "duel-end-draw", java.util.Map.of("opponent", challenger != null ? challenger.getName() : "Unknown"));
            endDuel(sessionId, null, 0, 0);
        });
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
                messages.send(opponent, "opponent-left", java.util.Map.of("opponent", player != null ? player.getName() : "Unknown"));
            }

            sessionManager.removeSession(session.id());
            playerStateManager.unregisterDuel(new Duel(session));
        });
    }

    public void endDuel(UUID sessionId, UUID winner, int challengerDamage, int targetDamage) {
        sessionManager.getSession(sessionId).ifPresent(session -> {
            sessionManager.updateSessionState(sessionId, DuelState.FINISHED);
            sessionManager.setEndedAt(sessionId, Instant.now());

            Player challenger = Bukkit.getPlayer(session.challenger().getUuid());
            Player target = Bukkit.getPlayer(session.target().getUuid());

            if (winner != null) {
                UUID loser = winner.equals(session.challenger().getUuid()) ? session.target().getUuid() : session.challenger().getUuid();
                Player winnerPlayer = Bukkit.getPlayer(winner);
                Player loserPlayer = Bukkit.getPlayer(loser);

                if (winnerPlayer != null) {
                    messages.send(winnerPlayer, "duel-end-win", java.util.Map.of("opponent", loserPlayer != null ? loserPlayer.getName() : "Unknown"));
                    rewardManager.giveRewards(winnerPlayer, session.kit().getName());
                    statisticManager.recordWin(winner, session.kit().getName()).join();
                }
                if (loserPlayer != null) {
                    messages.send(loserPlayer, "duel-end-lose", java.util.Map.of("opponent", winnerPlayer != null ? winnerPlayer.getName() : "Unknown"));
                    statisticManager.recordLoss(loser).join();
                }
            } else {
                if (challenger != null) messages.send(challenger, "duel-end-draw", java.util.Map.of("opponent", target != null ? target.getName() : "Unknown"));
                if (target != null) messages.send(target, "duel-end-draw", java.util.Map.of("opponent", challenger != null ? challenger.getName() : "Unknown"));
                if (challenger != null) statisticManager.recordDraw(challenger.getUniqueId()).join();
                if (target != null) statisticManager.recordDraw(target.getUniqueId()).join();
            }

            if (challenger != null) playerStateManager.restoreSnapshot(session.challenger().getUuid(), challenger);
            if (target != null) playerStateManager.restoreSnapshot(session.target().getUuid(), target);

            sessionManager.removeSession(sessionId);
            playerStateManager.unregisterDuel(new Duel(session));
        });
    }

    public void handleDuelDeath(Player player) {
        sessionManager.getSessionByPlayer(player.getUniqueId()).ifPresent(session -> {
            UUID opponentUuid = session.challenger().getUuid().equals(player.getUniqueId())
                    ? session.target().getUuid()
                    : session.challenger().getUuid();
            endDuel(session.id(), opponentUuid, 0, 0);
        });
    }

    public void expireRequest(UUID requestId) {
        requestManager.getRequest(requestId).ifPresent(request -> {
            Player challenger = Bukkit.getPlayer(request.challenger());
            Player target = Bukkit.getPlayer(request.target());
            if (challenger != null) messages.send(challenger, "target-denied", java.util.Map.of("target", target != null ? target.getName() : "Unknown"));
            if (target != null) messages.send(target, "request-expired");
            requestManager.removeRequest(requestId);
        });
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