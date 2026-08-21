package dev.itsdavlooo.coralduels.domain.duel;

import dev.itsdavlooo.coralduels.domain.arena.Arena;
import dev.itsdavlooo.coralduels.domain.kit.Kit;
import dev.itsdavlooo.coralduels.domain.player.DuelPlayer;

import java.time.Instant;
import java.util.UUID;

public final class Duel {

    private final UUID id;
    private final DuelPlayer challenger;
    private final DuelPlayer target;
    private final Kit kit;
    private final Arena arena;
    private DuelState state;
    private Instant startedAt;
    private Instant endedAt;
    private int countdownTaskId;
    private int duelTaskId;

    public Duel(UUID id, DuelPlayer challenger, DuelPlayer target, Kit kit, Arena arena) {
        this.id = id;
        this.challenger = challenger;
        this.target = target;
        this.kit = kit;
        this.arena = arena;
        this.state = DuelState.PENDING;
    }

    public UUID getId() { return id; }
    public DuelPlayer getChallenger() { return challenger; }
    public DuelPlayer getTarget() { return target; }
    public Kit getKit() { return kit; }
    public Arena getArena() { return arena; }
    public DuelState getState() { return state; }
    public void setState(DuelState state) { this.state = state; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public int getCountdownTaskId() { return countdownTaskId; }
    public void setCountdownTaskId(int countdownTaskId) { this.countdownTaskId = countdownTaskId; }
    public int getDuelTaskId() { return duelTaskId; }
    public void setDuelTaskId(int duelTaskId) { this.duelTaskId = duelTaskId; }

    public boolean isActive() {
        return state == DuelState.ACTIVE || state == DuelState.COUNTDOWN;
    }

    public DuelPlayer getOpponent(DuelPlayer player) {
        return player.equals(challenger) ? target : challenger;
    }
}