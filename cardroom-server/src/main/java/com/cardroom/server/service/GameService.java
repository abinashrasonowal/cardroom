package com.cardroom.server.service;

import com.cardroom.engine.GameCatalog;
import com.cardroom.server.dto.GameInfoResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** What can be played: whatever {@link GameCatalog} found on the classpath. */
@Service
public class GameService {

    private final GameCatalog catalog;

    public GameService(GameCatalog catalog) {
        this.catalog = catalog;
    }

    public List<GameInfoResponse> list() {
        return catalog.all().stream()
                .map(def -> new GameInfoResponse(def.id(), def.minPlayers(), def.maxPlayers()))
                .toList();
    }

    public boolean exists(String gameId) {
        return catalog.all().stream().anyMatch(def -> def.id().equals(gameId));
    }
}
