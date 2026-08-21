package dev.itsdavlooo.coralduels.domain.duel;

import dev.itsdavlooo.coralduels.domain.kit.Kit;
import dev.itsdavlooo.coralduels.domain.player.DuelPlayer;

import java.util.UUID;

public record DuelRequest(
        UUID id,
        UUID challenger,
        UUID target,
        Kit kit,
        long timestamp
) {
    public boolean isExpired(long timeoutSeconds) {
        return System.currentTimeMillis() - timestamp > timeoutSeconds * 1000L;
    }
}