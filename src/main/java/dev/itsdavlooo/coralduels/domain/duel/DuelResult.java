package dev.itsdavlooo.coralduels.domain.duel;

import java.util.UUID;

public record DuelResult(
        UUID duelId,
        UUID challenger,
        UUID target,
        UUID winner,
        String kitName,
        String arenaName,
        DuelState finalState,
        long durationTicks,
        int challengerDamage,
        int targetDamage,
        long startedAt,
        long endedAt
) {}