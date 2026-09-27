package com.cardroom.server.websocket;

import com.cardroom.contract.ErrorCode;
import com.cardroom.contract.PlayerId;
import com.cardroom.contract.RoomCode;
import com.cardroom.engine.Command;
import com.cardroom.engine.RoomActor;
import com.cardroom.engine.SocketId;
import com.cardroom.server.dto.frame.ClientFrame;
import com.cardroom.server.dto.frame.JoinPayload;
import com.cardroom.server.dto.frame.RejectedFrame;
import com.cardroom.server.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * The gateway's inbound side (§2.1): envelope parse → {@link Command} → room inbox. It knows
 * sockets and {@link PlayerId}, and no card rules — an intent's payload is handed to the room
 * untouched, and only the game's {@code parseIntent} ever reads it.
 */
@Component
public class GameSocketHandler extends TextWebSocketHandler {

    /** Sent when a room cannot take the command: closed, or its inbox is full. Clients do not retry. */
    static final CloseStatus ROOM_UNAVAILABLE = new CloseStatus(4000, "room unavailable");

    private final SocketHub hub;
    private final RoomService rooms;
    private final ObjectMapper json;

    public GameSocketHandler(SocketHub hub, RoomService rooms, ObjectMapper json) {
        this.hub = hub;
        this.rooms = rooms;
        this.json = json;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        hub.register(session, (PlayerId) session.getAttributes().get(IdentityHandshakeInterceptor.PLAYER));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        SocketHub.Conn conn = hub.get(session);
        if (conn == null) return;
        conn.lastSeen = System.currentTimeMillis();

        ClientFrame frame;
        try {
            frame = json.readValue(message.getPayload(), ClientFrame.class);
        } catch (Exception e) {
            reply(conn, null, ErrorCode.MALFORMED_INTENT, "frame is not a JSON envelope");
            return;
        }
        if (frame.v() != ClientFrame.PROTOCOL || frame.type() == null) {
            reply(conn, frame.id(), ErrorCode.MALFORMED_INTENT, "expected a v1 envelope {v, id, type, payload}");
            return;
        }

        if ("join".equals(frame.type())) {
            join(conn, frame);
            return;
        }
        Optional<RoomActor> room = Optional.ofNullable(conn.room).flatMap(rooms::find);
        if (room.isEmpty()) {
            reply(conn, frame.id(), ErrorCode.NOT_SEATED, "join a room first");
            return;
        }
        Command command = switch (frame.type()) {
            case "intent" -> new Command.Submit(conn.player, frame.payload(), frame.id());
            case "start" -> new Command.Start(conn.player);
            case "leave" -> new Command.Leave(conn.player);
            case "close" -> new Command.Close(conn.player);
            default -> null;
        };
        if (command == null) {
            reply(conn, frame.id(), ErrorCode.UNKNOWN_INTENT, "unknown frame type '" + frame.type() + "'");
            return;
        }
        offer(conn, frame.id(), room.get(), command);
    }

    private void join(SocketHub.Conn conn, ClientFrame frame) {
        JoinPayload join;
        try {
            join = frame.payload() == null ? null : json.treeToValue(frame.payload(), JoinPayload.class);
        } catch (Exception e) {
            join = null;
        }
        if (join == null || join.room() == null || join.room().isBlank()) {
            reply(conn, frame.id(), ErrorCode.MALFORMED_INTENT, "join needs a room code");
            return;
        }
        RoomCode code = new RoomCode(join.room().strip());
        if (conn.room != null && !conn.room.equals(code)) {
            // One socket, one room: otherwise a Disconnect would have to fan out to several.
            reply(conn, frame.id(), ErrorCode.MALFORMED_INTENT, "this socket already joined " + conn.room);
            return;
        }
        Optional<RoomActor> actor = rooms.find(code);
        if (actor.isEmpty()) {
            reply(conn, frame.id(), ErrorCode.ROOM_NOT_FOUND, "no room " + code);
            return;
        }
        conn.room = code;
        offer(conn, frame.id(), actor.get(), new Command.Join(conn.player,
                new SocketId(conn.session.getId()), join.nick(), join.clientSeed()));
    }

    private void offer(SocketHub.Conn conn, String id, RoomActor room, Command command) {
        if (!room.offer(command)) {
            reply(conn, id, ErrorCode.WRONG_PHASE, "room " + room.code() + " is closed or overloaded");
            hub.closeQuietly(conn, ROOM_UNAVAILABLE);
        }
    }

    private void reply(SocketHub.Conn conn, String id, ErrorCode error, String detail) {
        hub.send(conn, RejectedFrame.of(id, error, detail));
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) {
        hub.touch(session);
    }

    /** Dropping is not quitting: the seat is held and only this socket is released. */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SocketHub.Conn conn = hub.unregister(session);
        if (conn == null || conn.room == null) return;
        rooms.find(conn.room).ifPresent(room ->
                room.offer(new Command.Disconnect(conn.player, new SocketId(session.getId()))));
    }
}
