package dev.itsdavlooo.coralduels.domain.statistic;

public record PlayerStatistic(
        UUID uuid,
        String username,
        int duelsPlayed,
        int duelsWon,
        int duelsLost,
        int duelsDraw,
        int kills,
        int deaths,
        int currentStreak,
        int bestStreak,
        int eloRating,
        long totalDamageDealt,
        long totalDamageTaken,
        String favoriteKit,
        long lastDuelAt
) {}