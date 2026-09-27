package com.cardroom.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import com.cardroom.server.service.PlayerIdentityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** Milestone 1 end to end: HTTP create → two sockets join → start → draw, draw → reveal. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayIntegrationTest {

    private static final String SEED = "0123456789abcdef";

    @LocalServerPort int port;
    @Autowired TestRestTemplate http;
    @Autowired PlayerIdentityService cookies;
    @Autowired ObjectMapper json;

    private Client alice;
    private Client bob;

    @AfterEach
    void closeSockets() throws Exception {
        if (alice != null) alice.session.close();
        if (bob != null) bob.session.close();
    }

    @Test
    void highCardHandPlaysEndToEndWithoutLeakingCards() throws Exception {
        String room = createRoom();
        alice = connect(cookies.mint());
        bob = connect(cookies.mint());

        alice.send("join", Map.of("room", room, "nick", "alice", "clientSeed", SEED));
        alice.await(f -> isLobbyWith(f, 1));
        bob.send("join", Map.of("room", room, "nick", "bob", "clientSeed", SEED));
        JsonNode lobby = bob.await(f -> isLobbyWith(f, 2));
        assertEquals("high-card", lobby.at("/view/gameId").asText());
        assertEquals(room, lobby.at("/view/room").asText(), "RoomCode serializes as a bare string");

        alice.send("start", Map.of());
        JsonNode first = alice.await(f -> isGame(f));
        bob.await(f -> isGame(f));

        String onClock = first.at("/view/onClock").asText();
        Client drawer = onClock.equals(alice.playerId) ? alice : bob;
        Client other = drawer == alice ? bob : alice;

        drawer.send("intent", Map.of("type", "draw"));
        drawer.await(f -> "accepted".equals(f.path("type").asText()));
        JsonNode drawerView = drawer.await(f -> isGame(f) && seat(f, drawer.playerId).path("hasDrawn").asBoolean());
        JsonNode otherView = other.await(f -> isGame(f) && seat(f, drawer.playerId).path("hasDrawn").asBoolean());
        assertFalse(seat(drawerView, drawer.playerId).path("card").isNull(), "you see your own card");
        assertTrue(seat(otherView, drawer.playerId).path("card").isNull(), "nobody else does before the reveal");

        other.send("intent", Map.of("type", "draw"));
        JsonNode done = alice.await(f -> isGame(f) && f.at("/view/handComplete").asBoolean());
        assertNotNull(done.at("/view/winner").textValue());
        for (JsonNode seat : done.at("/view/seats")) {
            assertFalse(seat.path("card").isNull(), "every card is public once the hand is complete");
        }
    }

    @Test
    void outOfTurnIntentIsRejectedWithItsId() throws Exception {
        String room = createRoom();
        alice = connect(cookies.mint());
        bob = connect(cookies.mint());
        // First through the door is host, so alice must be seated before bob joins.
        alice.send("join", Map.of("room", room, "nick", "alice", "clientSeed", SEED));
        alice.await(f -> isLobbyWith(f, 1));
        bob.send("join", Map.of("room", room, "nick", "bob", "clientSeed", SEED));
        alice.await(f -> isLobbyWith(f, 2));
        alice.send("start", Map.of());
        String onClock = alice.await(f -> isGame(f)).at("/view/onClock").asText();
        Client waiting = onClock.equals(alice.playerId) ? bob : alice;

        String id = waiting.send("intent", Map.of("type", "draw"));
        JsonNode rejected = waiting.await(f -> "rejected".equals(f.path("type").asText()));
        assertEquals(id, rejected.path("re").asText());
        assertEquals("NOT_YOUR_TURN", rejected.path("error").asText());
    }

    @Test
    void malformedFramesAndUnknownRoomsAreRejectedNotFatal() throws Exception {
        alice = connect(cookies.mint());

        alice.session.sendMessage(new TextMessage("not json"));
        assertEquals("MALFORMED_INTENT", alice.await(f -> "rejected".equals(f.path("type").asText())).path("error").asText());

        alice.send("join", Map.of("room", "ZZZZZZ", "nick", "alice", "clientSeed", SEED));
        assertEquals("ROOM_NOT_FOUND", alice.await(f -> "rejected".equals(f.path("type").asText())).path("error").asText());

        alice.send("start", Map.of());
        assertEquals("NOT_SEATED", alice.await(f -> "rejected".equals(f.path("type").asText())).path("error").asText());
        assertTrue(alice.session.isOpen());
    }

    @Test
    void handshakeWithoutIdentityIsRefused() {
        assertThrows(Exception.class, () -> new StandardWebSocketClient()
                .execute(new TextWebSocketHandler(), new WebSocketHttpHeaders(), URI.create("ws://localhost:" + port + "/ws"))
                .get(5, TimeUnit.SECONDS));
    }

    @Test
    void firstHttpRequestIssuesACookieAndMeEchoesIt() {
        var response = http.getForEntity("/api/me", JsonNode.class);
        String setCookie = response.getHeaders().getFirst("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.startsWith(PlayerIdentityService.COOKIE + "="));
        assertTrue(setCookie.contains("HttpOnly"));
        String token = response.getBody().path("token").asText();
        assertEquals(response.getBody().path("playerId").asText(), cookies.verify(token).orElseThrow().value());
    }

    @Test
    void unknownGameIsA400WithTheErrorShape() {
        var response = http.postForEntity("/api/rooms", Map.of("gameId", "no-such-game"), JsonNode.class);
        assertEquals(400, response.getStatusCode().value());
        assertEquals("UNKNOWN_GAME", response.getBody().path("error").asText());
        assertEquals("no such game: no-such-game", response.getBody().path("detail").asText());
    }

    // ---- helpers ----

    private String createRoom() {
        JsonNode created = http.postForObject("/api/rooms", Map.of("gameId", "high-card"), JsonNode.class);
        return created.path("room").asText();
    }

    private Client connect(String token) throws Exception {
        Client client = new Client(cookies.verify(token).orElseThrow().value());
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Cookie", PlayerIdentityService.COOKIE + "=" + token);
        client.session = new StandardWebSocketClient()
                .execute(client, headers, URI.create("ws://localhost:" + port + "/ws"))
                .get(5, TimeUnit.SECONDS);
        return client;
    }

    private static boolean isLobbyWith(JsonNode frame, int members) {
        return "lobby".equals(frame.path("viewType").asText()) && frame.at("/view/members").size() == members;
    }

    private static boolean isGame(JsonNode frame) {
        return "game".equals(frame.path("viewType").asText());
    }

    private static JsonNode seat(JsonNode frame, String playerId) {
        for (JsonNode seat : frame.at("/view/seats")) {
            if (playerId.equals(seat.path("id").asText())) return seat;
        }
        throw new AssertionError("no seat for " + playerId + " in " + frame);
    }

    private final class Client extends TextWebSocketHandler {
        final String playerId;
        final BlockingQueue<JsonNode> frames = new LinkedBlockingQueue<>();
        WebSocketSession session;
        int nextId;

        Client(String playerId) {
            this.playerId = playerId;
        }

        @Override
        protected void handleTextMessage(WebSocketSession s, TextMessage message) throws Exception {
            frames.add(json.readTree(message.getPayload()));
        }

        String send(String type, Map<String, ?> payload) throws Exception {
            String id = "c" + (++nextId);
            session.sendMessage(new TextMessage(json.writeValueAsString(
                    Map.of("v", 1, "id", id, "type", type, "payload", payload))));
            return id;
        }

        /** Skips frames until one matches; the room broadcasts more updates than any test asks about. */
        JsonNode await(Predicate<JsonNode> match) throws InterruptedException {
            long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while (true) {
                JsonNode frame = frames.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
                if (frame == null) throw new AssertionError("timed out waiting for a frame");
                if (match.test(frame)) return frame;
            }
        }
    }
}
