package dev.itsdavlooo.coralduels.domain.duel;

import dev.itsdavlooo.coralduels.domain.arena.Arena;
import dev.itsdavlooo.coralduels.domain.kit.Kit;
import dev.itsdavlooo.coralduels.domain.player.DuelPlayer;

import java.time.Instant;
import java.util.UUID;

public record DuelSession(
        UUID id,
        DuelPlayer challenger,
        DuelPlayer target,
        Kit kit,
        Arena arena,
        DuelState state,
        Instant startedAt,
        Instant endedAt
) {
    public boolean isActive() {
        return state == DuelState.ACTIVE || state == DuelState.COUNTDOWN;
    }

    public DuelPlayer getOpponent(DuelPlayer player) {
        return player.equals(challenger) ? target : challenger;
    }
}