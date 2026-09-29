package com.whitejack.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The one JVM. Spring lives only under this package:
 *
 * <pre>
 * config/      engine, WebSocket and JSON wiring
 * controller/  the small REST surface (/api/me, /api/games, /api/rooms, /api/rooms/{room}/bots)
 * service/     identity, game catalog, room lifecycle and bots
 * websocket/   the gateway: inbound frames → commands, and the engine's Broadcaster
 * dto/         REST bodies, and dto/frame for the §7 wire protocol
 * filter/      identity cookie on the first HTTP request
 * exception/   one error shape for REST
 * </pre>
 *
 * engine-core and the games are plain Java, built in {@code config.EngineConfig}, never scanned.
 */
@SpringBootApplication
public class WhitejackApplication {
    public static void main(String[] args) {
        SpringApplication.run(WhitejackApplication.class, args);
    }
}
