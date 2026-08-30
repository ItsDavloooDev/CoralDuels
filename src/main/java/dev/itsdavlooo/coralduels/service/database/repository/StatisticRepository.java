package dev.itsdavlooo.coralduels.service.database.repository;

import dev.itsdavlooo.coralduels.domain.statistic.LeaderboardEntry;
import dev.itsdavlooo.coralduels.domain.statistic.PlayerStatistic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StatisticRepository {

    CompletableFuture<Optional<PlayerStatistic>> findByUuid(UUID uuid);

    CompletableFuture<PlayerStatistic> createOrUpdate(PlayerStatistic statistic);

    CompletableFuture<List<LeaderboardEntry>> getLeaderboard(String category, int limit);

    CompletableFuture<Void> incrementStat(UUID uuid, String column, int amount);

    CompletableFuture<Void> updateElo(UUID uuid, int newElo);

    CompletableFuture<Void> updateStreak(UUID uuid, int streak);

    CompletableFuture<Void> recordDuelHistory(DuelHistoryRecord record);

    CompletableFuture<Void> recordMatchResult(UUID winner, UUID loser, String kitName, int winnerDamage, int loserDamage);

    CompletableFuture<Void> recordDrawResult(UUID player1, UUID player2, String kitName, int damage1, int damage2);

    record DuelHistoryRecord(
            UUID duelId,
            UUID challenger,
            UUID target,
            UUID winner,
            String kitName,
            String arenaName,
            String state,
            int durationTicks,
            int challengerDamage,
            int targetDamage,
            long startedAt,
            long endedAt
    ) {}
}