package com.whitejack.server.websocket;

import com.whitejack.contract.PlayerId;
import com.whitejack.server.service.PlayerIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/** Cookie first; {@code ?t=} is the §4 fallback for browsers that withhold the cookie. */
@Component
public class IdentityHandshakeInterceptor implements HandshakeInterceptor {

    /** Session attribute holding the verified {@link PlayerId}. */
    static final String PLAYER = "whitejack.player";

    private final PlayerIdentityService identity;

    public IdentityHandshakeInterceptor(PlayerIdentityService identity) {
        this.identity = identity;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servlet)) return false;
        HttpServletRequest http = servlet.getServletRequest();
        Optional<PlayerId> player = identity.validTokenFrom(http)
                .or(() -> Optional.ofNullable(http.getParameter("t")))
                .flatMap(identity::verify);
        if (player.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(PLAYER, player.get());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {}
}
