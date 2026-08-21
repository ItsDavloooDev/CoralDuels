package dev.itsdavlooo.coralduels.domain.statistic;

public record LeaderboardEntry(
        int position,
        UUID uuid,
        String username,
        int wins,
        int losses,
        int elo,
        String kit
) {}