package com.cardroom.server.config;

import com.cardroom.engine.Broadcaster;
import com.cardroom.engine.GameCatalog;
import com.cardroom.engine.RoomRegistry;
import com.cardroom.engine.TurnClock;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Builds the engine from plain constructors — engine-core and the games are never scanned.
 * The {@link Broadcaster} is the WebSocket layer's {@code SocketHub}, the engine's only port out.
 */
@Configuration
@EnableScheduling
public class EngineConfig {

    @Bean
    public GameCatalog gameCatalog() {
        return GameCatalog.load();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean(destroyMethod = "close")
    public TurnClock turnClock() {
        return new TurnClock();
    }

    @Bean(destroyMethod = "close")
    public RoomRegistry roomRegistry(GameCatalog catalog, Broadcaster broadcaster, TurnClock turnClock, Clock clock) {
        return new RoomRegistry(catalog::require, broadcaster, turnClock, clock);
    }
}
