package dev.itsdavlooo.coralduels.domain.statistic;

import dev.itsdavlooo.coralduels.service.database.repository.StatisticRepository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class StatisticManager {

    private final StatisticRepository repository;

    public StatisticManager(StatisticRepository repository) {
        this.repository = repository;
    }

    public CompletableFuture<PlayerStatistic> getOrCreateStatistic(UUID uuid, String username) {
        return repository.findByUuid(uuid)
                .thenCompose(opt -> opt.map(CompletableFuture::completedFuture)
                        .orElseGet(() -> repository.createOrUpdate(new PlayerStatistic(
                                uuid, username, 0, 0, 0, 0, 0, 0, 0, 0, 1000, 0, 0, "", 0
                        )))
                );
    }

    public CompletableFuture<PlayerStatistic> updateStatistic(PlayerStatistic statistic) {
        return repository.createOrUpdate(statistic);
    }

    public CompletableFuture<List<LeaderboardEntry>> getLeaderboard(String category, int limit) {
        return repository.getLeaderboard(category, limit);
    }

    public CompletableFuture<Void> recordWin(UUID uuid, String kitName) {
        return repository.incrementStat(uuid, "duels_won", 1)
                .thenCompose(v -> repository.incrementStat(uuid, "duels_played", 1))
                .thenCompose(v -> repository.incrementStat(uuid, "current_streak", 1))
                .thenCompose(v -> repository.incrementStat(uuid, "kills", 1));
    }

    public CompletableFuture<Void> recordLoss(UUID uuid) {
        return repository.incrementStat(uuid, "duels_lost", 1)
                .thenCompose(v -> repository.incrementStat(uuid, "duels_played", 1))
                .thenCompose(v -> repository.updateStreak(uuid, 0))
                .thenCompose(v -> repository.incrementStat(uuid, "deaths", 1));
    }

    public CompletableFuture<Void> recordDraw(UUID uuid) {
        return repository.incrementStat(uuid, "duels_draw", 1)
                .thenCompose(v -> repository.incrementStat(uuid, "duels_played", 1))
                .thenCompose(v -> repository.updateStreak(uuid, 0));
    }

    public CompletableFuture<Void> recordDamage(UUID uuid, long dealt, long taken) {
        return repository.incrementStat(uuid, "total_damage_dealt", (int) dealt)
                .thenCompose(v -> repository.incrementStat(uuid, "total_damage_taken", (int) taken));
    }

    public CompletableFuture<Void> updateElo(UUID uuid, int newElo) {
        return repository.updateElo(uuid, newElo);
    }

    public CompletableFuture<Void> recordDuelHistory(dev.itsdavlooo.coralduels.service.database.repository.StatisticRepository.DuelHistoryRecord record) {
        return repository.recordDuelHistory(record);
    }
}