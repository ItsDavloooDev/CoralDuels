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

    public CompletableFuture<Void> recordMatchResult(UUID winner, UUID loser, String kitName, int winnerDamage, int loserDamage) {
        return repository.recordMatchResult(winner, loser, kitName, winnerDamage, loserDamage);
    }

    public CompletableFuture<Void> recordDrawResult(UUID player1, UUID player2, String kitName, int damage1, int damage2) {
        return repository.recordDrawResult(player1, player2, kitName, damage1, damage2);
    }

    public CompletableFuture<Void> recordDamage(UUID uuid, long dealt, long taken) {
        return repository.incrementStat(uuid, "total_damage_dealt", (int) dealt)
                .thenCompose(v -> repository.incrementStat(uuid, "total_damage_taken", (int) taken));
    }

    public CompletableFuture<Void> updateElo(UUID uuid, int newElo) {
        return repository.updateElo(uuid, newElo);
    }

    public CompletableFuture<Void> updateStreak(UUID uuid, int streak) {
        return repository.updateStreak(uuid, streak);
    }

    public CompletableFuture<Void> recordDuelHistory(dev.itsdavlooo.coralduels.service.database.repository.StatisticRepository.DuelHistoryRecord record) {
        return repository.recordDuelHistory(record);
    }

    public void showStats(Player viewer, UUID targetUuid) {
        getOrCreateStatistic(targetUuid, Bukkit.getOfflinePlayer(targetUuid).getName())
                .thenAcceptAsync(stat -> {
                    if (!viewer.isOnline()) {
                        return;
                    }
                    String targetName = stat.username();
                    if (viewer.getUniqueId().equals(targetUuid)) {
                        targetName = "Tu";
                    }
                    messages.send(viewer, "stats-header");
                    messages.send(viewer, "stats-format", Map.of("stat", "Vittorie", "value", String.valueOf(stat.duelsWon())));
                    messages.send(viewer, "stats-format", Map.of("stat", "Sconfitte", "value", String.valueOf(stat.duelsLost())));
                    messages.send(viewer, "stats-format", Map.of("stat", "Pareggi", "value", String.valueOf(stat.duelsDraw())));
                    messages.send(viewer, "stats-format", Map.of("stat", "Giocate", "value", String.valueOf(stat.duelsPlayed())));
                    messages.send(viewer, "stats-format", Map.of("stat", "ELO", "value", String.valueOf(stat.eloRating())));
                    messages.send(viewer, "stats-format", Map.of("stat", "Streak", "value", String.valueOf(stat.currentStreak())));
                    messages.send(viewer, "stats-format", Map.of("stat", "Kill", "value", String.valueOf(stat.kills())));
                    messages.send(viewer, "stats-format", Map.of("stat", "Morti", "value", String.valueOf(stat.deaths())));
                    messages.send(viewer, "stats-format", Map.of("stat", "Danno inflitto", "value", String.valueOf(stat.totalDamageDealt())));
                    messages.send(viewer, "stats-format", Map.of("stat", "Danno subito", "value", String.valueOf(stat.totalDamageTaken())));
                }, runnable -> plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> runnable.run()))
                .exceptionally(e -> {
                    if (viewer.isOnline()) {
                        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> messages.send(viewer, "database-error"));
                    }
                    return null;
                });
    }

    public void showLeaderboard(Player viewer, String category) {
        String normalized = switch (category.toLowerCase()) {
            case "wins", "vittorie" -> "wins";
            case "losses", "sconfitte" -> "losses";
            case "played", "giocate" -> "played";
            case "streak", "striscia" -> "streak";
            case "kills", "uccisioni" -> "kills";
            case "deaths", "morti" -> "deaths";
            default -> "elo";
        };
        getLeaderboard(normalized, 10)
                .thenAcceptAsync(entries -> {
                    if (!viewer.isOnline()) {
                        return;
                    }
                    messages.send(viewer, "leaderboard-header");
                    if (entries.isEmpty()) {
                        messages.send(viewer, "leaderboard-empty");
                        return;
                    }
                    int pos = 1;
                    for (LeaderboardEntry entry : entries) {
                        messages.send(viewer, "leaderboard-format", Map.of(
                                "pos", String.valueOf(pos),
                                "player", entry.username(),
                                "wins", String.valueOf(entry.wins()),
                                "losses", String.valueOf(entry.losses()),
                                "elo", String.valueOf(entry.elo())
                        ));
                        pos++;
                    }
                }, runnable -> plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> runnable.run()))
                .exceptionally(e -> {
                    if (viewer.isOnline()) {
                        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> messages.send(viewer, "database-error"));
                    }
                    return null;
                });
    }
}