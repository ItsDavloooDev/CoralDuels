package dev.itsdavlooo.coralduels.domain.duel;

import dev.itsdavlooo.coralduels.domain.kit.Kit;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RequestManager {

    private final Map<UUID, DuelRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, DuelRequest> requestsByTarget = new ConcurrentHashMap<>();

    public DuelRequest addRequest(UUID challenger, UUID target, Kit kit) {
        DuelRequest request = new DuelRequest(UUID.randomUUID(), challenger, target, kit, System.currentTimeMillis());
        DuelRequest existing = pendingRequests.putIfAbsent(request.id(), request);
        if (existing != null) {
            return existing;
        }
        DuelRequest existingTarget = requestsByTarget.putIfAbsent(target, request);
        if (existingTarget != null) {
            pendingRequests.remove(request.id());
            return existingTarget;
        }
        return request;
    }

    public Optional<DuelRequest> getRequest(UUID id) {
        return Optional.ofNullable(pendingRequests.get(id));
    }

    public Optional<DuelRequest> getRequestByTarget(UUID target) {
        return Optional.ofNullable(requestsByTarget.get(target));
    }

    public void removeRequest(UUID id) {
        DuelRequest request = pendingRequests.remove(id);
        if (request != null) {
            requestsByTarget.remove(request.target());
        }
    }

    public void removeRequestByTarget(UUID target) {
        DuelRequest request = requestsByTarget.remove(target);
        if (request != null) {
            pendingRequests.remove(request.id());
        }
    }

    public boolean hasPendingRequest(UUID player) {
        return requestsByTarget.containsKey(player) || pendingRequests.values().stream()
                .anyMatch(r -> r.challenger().equals(player));
    }

    public void clear() {
        pendingRequests.clear();
        requestsByTarget.clear();
    }

    public java.util.Collection<DuelRequest> getPendingRequests() {
        return pendingRequests.values();
    }
}