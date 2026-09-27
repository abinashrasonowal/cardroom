package com.cardroom.server.websocket;

import com.cardroom.contract.PlayerId;
import com.cardroom.contract.PlayerView;
import com.cardroom.contract.RoomCode;
import com.cardroom.engine.Broadcaster;
import com.cardroom.server.dto.frame.ServerFrame;
import com.cardroom.server.dto.frame.UpdateFrame;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

/**
 * Every open socket, and the engine's {@link Broadcaster}. Room threads call in concurrently,
 * so each session is wrapped in a decorator that serialises sends — Tomcat's raw session
 * throws if two threads write at once.
 *
 * <p>Sends are best-effort: a slow or dead socket is closed, never allowed to block a room
 * thread, because that thread is every other player's game too.
 */
@Component
public class SocketHub implements Broadcaster {

    private static final Logger LOG = LoggerFactory.getLogger(SocketHub.class);
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_LIMIT_BYTES = 512 * 1024;
    private static final long DROP_AFTER_MS = 45_000;

    /** One socket: who it is, and which room it joined (null until its first {@code join}). */
    static final class Conn {
        final WebSocketSession session;
        final PlayerId player;
        volatile RoomCode room;
        volatile long lastSeen = System.currentTimeMillis();

        Conn(WebSocketSession session, PlayerId player) {
            this.session = session;
            this.player = player;
        }
    }

    private final ObjectMapper json;
    private final Map<String, Conn> bySession = new ConcurrentHashMap<>();
    private final Map<PlayerId, Set<Conn>> byPlayer = new ConcurrentHashMap<>();

    public SocketHub(ObjectMapper json) {
        this.json = json;
    }

    Conn register(WebSocketSession raw, PlayerId player) {
        Conn conn = new Conn(new ConcurrentWebSocketSessionDecorator(raw, SEND_TIME_LIMIT_MS, BUFFER_LIMIT_BYTES), player);
        bySession.put(raw.getId(), conn);
        byPlayer.computeIfAbsent(player, p -> ConcurrentHashMap.newKeySet()).add(conn);
        return conn;
    }

    Conn get(WebSocketSession raw) {
        return bySession.get(raw.getId());
    }

    Conn unregister(WebSocketSession raw) {
        Conn conn = bySession.remove(raw.getId());
        if (conn != null) {
            byPlayer.computeIfPresent(conn.player, (p, set) -> {
                set.remove(conn);
                return set.isEmpty() ? null : set;
            });
        }
        return conn;
    }

    // ---- Broadcaster ----

    @Override
    public void toRoom(RoomCode code, Map<PlayerId, PlayerView> views) {
        views.forEach((player, view) -> {
            UpdateFrame frame = UpdateFrame.of(code, view);
            for (Conn conn : byPlayer.getOrDefault(player, Set.of())) {
                if (code.equals(conn.room)) send(conn, frame);
            }
        });
    }

    /**
     * ponytail: the port carries no room code, so a player with sockets in two rooms gets the
     * reply on all of them. The client ignores {@code re} ids it did not send; a room-scoped
     * {@code toPlayer} is the fix if multi-room play ever matters.
     */
    @Override
    public void toPlayer(PlayerId player, Object message) {
        ServerFrame frame = FrameMapper.toFrame(message);
        for (Conn conn : byPlayer.getOrDefault(player, Set.of())) send(conn, frame);
    }

    void send(Conn conn, ServerFrame frame) {
        if (!conn.session.isOpen()) return;
        try {
            conn.session.sendMessage(new TextMessage(json.writeValueAsString(frame)));
        } catch (IOException | IllegalStateException e) {
            LOG.debug("dropping socket {}: {}", conn.session.getId(), e.toString());
            closeQuietly(conn, CloseStatus.SESSION_NOT_RELIABLE);
        }
    }

    // ---- heartbeat (§7): ping every 15s, drop at 45s ----

    void touch(WebSocketSession raw) {
        Conn conn = get(raw);
        if (conn != null) conn.lastSeen = System.currentTimeMillis();
    }

    @Scheduled(fixedRate = 15_000)
    public void heartbeat() {
        long cutoff = System.currentTimeMillis() - DROP_AFTER_MS;
        for (Conn conn : bySession.values()) {
            if (conn.lastSeen < cutoff) {
                closeQuietly(conn, CloseStatus.SESSION_NOT_RELIABLE);
                continue;
            }
            try {
                conn.session.sendMessage(new PingMessage(ByteBuffer.allocate(0)));
            } catch (IOException | IllegalStateException e) {
                closeQuietly(conn, CloseStatus.SESSION_NOT_RELIABLE);
            }
        }
    }

    void closeQuietly(Conn conn, CloseStatus status) {
        try {
            conn.session.close(status);
        } catch (IOException ignored) {
            // already gone; afterConnectionClosed does the bookkeeping
        }
    }
}
