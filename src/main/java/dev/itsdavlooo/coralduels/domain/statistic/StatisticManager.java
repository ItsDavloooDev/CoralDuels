package dev.itsdavlooo.coralduels.domain.statistic;

import dev.itsdavlooo.coralduels.CoralDuelsPlugin;
import dev.itsdavlooo.coralduels.service.database.repository.StatisticRepository;
import dev.itsdavlooo.coralduels.service.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class StatisticManager {

    private final StatisticRepository repository;
    private final CoralDuelsPlugin plugin;
    private final MessageService messages;

    public StatisticManager(StatisticRepository repository) {
        this.repository = repository;
        this.plugin = CoralDuelsPlugin.getInstance();
        this.messages = plugin.getMessageService();
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

    public void showStats(Player viewer, UUID targetUuid) {
        getOrCreateStatistic(targetUuid, Bukkit.getOfflinePlayer(targetUuid).getName())
                .thenAccept(stat -> {
                    String targetName = stat.getUsername();
                    if (viewer.getUniqueId().equals(targetUuid)) {
                        targetName = "Tu";
                    }
                    messages.send(viewer, "stats-header");
                    messages.send(viewer, "stats-format", Map.of("stat", "Vittorie", "value", String.valueOf(stat.getWins())));
                    messages.send(viewer, "stats-format", Map.of("stat", "Sconfitte", "value", String.valueOf(stat.getLosses())));
                    messages.send(viewer, "stats-format", Map.of("stat", "Pareggi", "value", String.valueOf(stat.getDraws())));
                    messages.send(viewer, "stats-format", Map.of("stat", "Giocate", "value", String.valueOf(stat.getDuelsPlayed())));
                    messages.send(viewer, "stats-format", Map.of("stat", "ELO", "value", String.valueOf(stat.getElo())));
                    messages.send(viewer, "stats-format", Map.of("stat", "Streak", "value", String.valueOf(stat.getCurrentStreak())));
                    messages.send(viewer, "stats-format", Map.of("stat", "Kill", "value", String.valueOf(stat.getKills())));
                    messages.send(viewer, "stats-format", Map.of("stat", "Morti", "value", String.valueOf(stat.getDeaths())));
                    messages.send(viewer, "stats-format", Map.of("stat", "Danno inflitto", "value", String.valueOf(stat.getTotalDamageDealt())));
                    messages.send(viewer, "stats-format", Map.of("stat", "Danno subito", "value", String.valueOf(stat.getTotalDamageTaken())));
                })
                .exceptionally(e -> {
                    messages.send(viewer, "database-error");
                    return null;
                });
    }

    public void showLeaderboard(Player viewer) {
        getLeaderboard("elo", 10)
                .thenAccept(entries -> {
                    messages.send(viewer, "leaderboard-header");
                    int pos = 1;
                    for (LeaderboardEntry entry : entries) {
                        messages.send(viewer, "leaderboard-format", Map.of(
                                "pos", String.valueOf(pos),
                                "player", entry.getUsername(),
                                "wins", String.valueOf(entry.getWins()),
                                "elo", String.valueOf(entry.getElo())
                        ));
                        pos++;
                    }
                })
                .exceptionally(e -> {
                    messages.send(viewer, "database-error");
                    return null;
                });
    }
}