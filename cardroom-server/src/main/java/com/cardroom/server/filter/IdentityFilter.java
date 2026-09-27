package com.cardroom.server.filter;

import com.cardroom.server.service.PlayerIdentityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * First HTTP request → signed cookie (§4). The WebSocket handshake is excluded: it must
 * <em>present</em> an identity, and minting one there would hide a missing cookie.
 */
@Component
public class IdentityFilter extends OncePerRequestFilter {

    /** Request attribute holding the caller's verified token. */
    public static final String TOKEN = "cardroom.token";

    /** The §4 fallback: the client's localStorage mirror of the token, for when the cookie is withheld. */
    public static final String TOKEN_HEADER = "X-Cardroom-Token";

    private final PlayerIdentityService identity;

    public IdentityFilter(PlayerIdentityService identity) {
        this.identity = identity;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/ws");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = identity.validTokenFrom(request).orElse(null);
        if (token == null) {
            String mirrored = request.getHeader(TOKEN_HEADER);
            token = identity.verify(mirrored).isPresent() ? mirrored : identity.mint();
            response.addHeader(HttpHeaders.SET_COOKIE, identity.cookie(token, request.isSecure()).toString());
        }
        request.setAttribute(TOKEN, token);
        chain.doFilter(request, response);
    }
}
