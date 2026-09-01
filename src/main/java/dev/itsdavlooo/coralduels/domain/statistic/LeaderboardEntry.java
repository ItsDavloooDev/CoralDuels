package dev.itsdavlooo.coralduels.domain.statistic;

import java.util.UUID;

public record LeaderboardEntry(
        int position,
        UUID uuid,
        String username,
        int wins,
        int losses,
        int elo,
        int played,
        int streak,
        int kills,
        int deaths,
        String kit
) {}
