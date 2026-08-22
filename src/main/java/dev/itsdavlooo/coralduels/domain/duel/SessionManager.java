package dev.itsdavlooo.coralduels.domain.duel;

import dev.itsdavlooo.coralduels.domain.player.DuelPlayer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager {

    private final Map<UUID, DuelSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToSession = new ConcurrentHashMap<>();

    public void addSession(DuelSession session) {
        sessions.put(session.id(), session);
        playerToSession.put(session.challenger().getUuid(), session.id());
        playerToSession.put(session.target().getUuid(), session.id());
    }

    public Optional<DuelSession> getSession(UUID id) {
        return Optional.ofNullable(sessions.get(id));
    }

    public Optional<DuelSession> getSessionByPlayer(UUID player) {
        UUID sessionId = playerToSession.get(player);
        return sessionId != null ? Optional.ofNullable(sessions.get(sessionId)) : Optional.empty();
    }

    public void removeSession(UUID id) {
        DuelSession session = sessions.remove(id);
        if (session != null) {
            playerToSession.remove(session.challenger().getUuid());
            playerToSession.remove(session.target().getUuid());
        }
    }

    public void removeSessionByPlayer(UUID player) {
        UUID sessionId = playerToSession.remove(player);
        if (sessionId != null) {
            DuelSession session = sessions.remove(sessionId);
            if (session != null) {
                UUID opponentUuid = session.challenger().getUuid().equals(player)
                        ? session.target().getUuid()
                        : session.challenger().getUuid();
                playerToSession.remove(opponentUuid);
            }
        }
    }

    public boolean hasActiveSession(UUID player) {
        return getSessionByPlayer(player).map(DuelSession::isActive).orElse(false);
    }

    public void updateSessionState(UUID id, DuelState state) {
        sessions.computeIfPresent(id, (k, s) -> new DuelSession(
                s.id(), s.challenger(), s.target(), s.kit(), s.arena(), state, s.startedAt(), s.endedAt()
        ));
    }

    public void setStartedAt(UUID id, java.time.Instant startedAt) {
        sessions.computeIfPresent(id, (k, s) -> new DuelSession(
                s.id(), s.challenger(), s.target(), s.kit(), s.arena(), s.state(), startedAt, s.endedAt()
        ));
    }

    public void setEndedAt(UUID id, java.time.Instant endedAt) {
        sessions.computeIfPresent(id, (k, s) -> new DuelSession(
                s.id(), s.challenger(), s.target(), s.kit(), s.arena(), s.state(), s.startedAt(), endedAt
        ));
    }

    public void clear() {
        sessions.clear();
        playerToSession.clear();
    }
}