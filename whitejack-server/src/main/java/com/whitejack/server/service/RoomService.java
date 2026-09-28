package com.whitejack.server.service;

import com.whitejack.contract.RoomCode;
import com.whitejack.engine.RoomActor;
import com.whitejack.engine.RoomRegistry;
import com.whitejack.server.exception.InvalidRequestException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Room lifecycle on top of {@link RoomRegistry}: create, look up, and reap idle rooms. */
@Service
public class RoomService {

    private final RoomRegistry registry;
    private final GameService games;
    private final Duration idleGrace;

    public RoomService(RoomRegistry registry, GameService games,
            @Value("${whitejack.room-idle:30m}") Duration idleGrace) {
        this.registry = registry;
        this.games = games;
        this.idleGrace = idleGrace;
    }

    public RoomCode create(String gameId) {
        if (gameId == null || gameId.isBlank()) {
            throw new InvalidRequestException("INVALID_REQUEST", "gameId is required");
        }
        if (!games.exists(gameId)) {
            throw new InvalidRequestException("UNKNOWN_GAME", "no such game: " + gameId);
        }
        return registry.create(gameId, Map.of()).code();
    }

    public Optional<RoomActor> find(RoomCode code) {
        return registry.find(code);
    }

    /** {@link RoomRegistry#reapIdle} deliberately has no timer of its own; this is it. */
    @Scheduled(fixedDelay = 60_000)
    public void reapIdle() {
        registry.reapIdle(idleGrace);
    }
}
