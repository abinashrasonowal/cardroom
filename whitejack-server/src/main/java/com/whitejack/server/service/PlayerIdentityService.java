package com.whitejack.server.service;

import com.whitejack.contract.PlayerId;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/**
 * Identity without accounts (§4): the cookie <em>is</em> the session. A token is
 * {@code <uuid>.<base64url(HMAC-SHA256(secret, uuid))>}, so a client can mint nothing and can
 * only present a token this server signed.
 */
@Service
public class PlayerIdentityService {

    public static final String COOKIE = "cr_pid";
    private static final Logger LOG = LoggerFactory.getLogger(PlayerIdentityService.class);
    private static final Duration MAX_AGE = Duration.ofDays(365);

    private final SecretKeySpec key;

    public PlayerIdentityService(@Value("${whitejack.cookie-secret:}") String secret) {
        byte[] bytes;
        if (secret.isBlank()) {
            // Fine for dev; in production every restart would orphan every seat.
            LOG.warn("whitejack.cookie-secret is not set; using a random per-boot secret");
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.key = new SecretKeySpec(bytes, "HmacSHA256");
    }

    public String mint() {
        return tokenFor(new PlayerId(UUID.randomUUID().toString()));
    }

    public String tokenFor(PlayerId player) {
        return player.value() + "." + sign(player.value());
    }

    public Optional<PlayerId> verify(String token) {
        if (token == null) return Optional.empty();
        int dot = token.indexOf('.');
        if (dot <= 0 || dot == token.length() - 1) return Optional.empty();
        String id = token.substring(0, dot);
        byte[] expected = sign(id).getBytes(StandardCharsets.US_ASCII);
        byte[] actual = token.substring(dot + 1).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, actual) ? Optional.of(new PlayerId(id)) : Optional.empty();
    }

    /** The token from the request's cookie, if it carries a valid one. */
    public Optional<String> validTokenFrom(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        for (Cookie cookie : cookies) {
            if (COOKIE.equals(cookie.getName()) && verify(cookie.getValue()).isPresent()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    public ResponseCookie cookie(String token, boolean secure) {
        return ResponseCookie.from(COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(MAX_AGE)
                .build();
    }

    private String sign(String id) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] tag = mac.doFinal(id.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(tag);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }
}
