package com.cardroom.server.config;

import com.cardroom.server.websocket.GameSocketHandler;
import com.cardroom.server.websocket.IdentityHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** {@code /ws}: identity is verified at the handshake, so every frame after it has a PlayerId. */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameSocketHandler handler;
    private final IdentityHandshakeInterceptor identity;
    private final String[] allowedOrigins;

    /**
     * @param allowedOrigins cross-origin handshakes to accept; same-origin is always allowed.
     *     The default admits the Vite dev server on :3000.
     */
    public WebSocketConfig(GameSocketHandler handler, IdentityHandshakeInterceptor identity,
            @Value("${cardroom.allowed-origins:http://localhost:*}") String[] allowedOrigins) {
        this.handler = handler;
        this.identity = identity;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws")
                .addInterceptors(identity)
                .setAllowedOriginPatterns(allowedOrigins);
    }
}
