package com.whitejack.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import com.whitejack.server.service.PlayerIdentityService;
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
        assertEquals("high-card", lobby.path("game").asText(), "every view frame names its game");

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
    void heartsSeatsFourPlayersAndDealsEachThirteenPrivateCards() throws Exception {
        String room = createRoom("hearts");
        List<Client> players = new ArrayList<>();
        try {
            for (String nick : List.of("north", "east", "south", "west")) {
                Client player = connect(cookies.mint());
                players.add(player);
                player.send("join", Map.of("room", room, "nick", nick, "clientSeed", SEED));
                // Joined one at a time, so the first is host and seat order is join order.
                players.get(0).await(f -> isLobbyWith(f, players.size()));
                if (players.size() == 3) {
                    players.get(0).send("start", Map.of());
                    assertEquals("NOT_ENOUGH_PLAYERS",
                            players.get(0).await(f -> "rejected".equals(f.path("type").asText())).path("error").asText());
                }
            }
            players.get(0).send("start", Map.of());

            Set<String> dealt = new HashSet<>();
            List<JsonNode> views = new ArrayList<>();
            for (Client player : players) {
                JsonNode frame = player.await(f -> isGame(f));
                assertEquals("hearts", frame.path("game").asText());
                JsonNode view = frame.path("view");
                assertEquals("PASSING", view.path("phase").asText());
                assertEquals(13, view.path("myHand").size());
                view.path("myHand").forEach(card -> dealt.add(card.toString()));
                view.path("seats").forEach(seat -> assertEquals(13, seat.path("cardCount").asInt()));
                assertFalse(view.path("seats").get(0).has("cards"), "other hands are counts, never cards");
                views.add(view);
            }
            assertEquals(52, dealt.size(), "four private hands, no card seen twice");

            for (int i = 0; i < 4; i++) {
                JsonNode hand = views.get(i).path("myHand");
                players.get(i).send("intent", Map.of("type", "pass", "cards", List.of(hand.get(0), hand.get(1), hand.get(2))));
            }
            JsonNode playing = players.get(0).await(f -> isGame(f) && "PLAYING".equals(f.at("/view/phase").asText()));
            String onClock = playing.at("/view/onClock").asText();
            Client leader = players.stream().filter(p -> p.playerId.equals(onClock)).findFirst().orElseThrow();
            // players.get(0) already consumed its PLAYING view just above; waiting again would hang.
            JsonNode leaderView = leader == players.get(0) ? playing
                    : leader.await(f -> isGame(f) && "PLAYING".equals(f.at("/view/phase").asText()));
            assertEquals("TWO", leaderView.at("/view/legal/0/rank").asText(), "the 2♣ holder leads, and only the 2♣");

            leader.send("intent", Map.of("type", "play", "card", leaderView.at("/view/legal/0")));
            leader.await(f -> "accepted".equals(f.path("type").asText()));
        } finally {
            for (Client player : players) player.session.close();
        }
    }

    @Test
    void ginRummyDealsTenPrivateCardsAndPlaysADrawThenDiscard() throws Exception {
        String room = createRoom("gin-rummy");
        alice = connect(cookies.mint());
        bob = connect(cookies.mint());
        alice.send("join", Map.of("room", room, "nick", "alice", "clientSeed", SEED));
        alice.await(f -> isLobbyWith(f, 1));
        bob.send("join", Map.of("room", room, "nick", "bob", "clientSeed", SEED));
        alice.await(f -> isLobbyWith(f, 2));
        alice.send("start", Map.of());

        JsonNode aliceView = alice.await(f -> isGame(f)).path("view");
        JsonNode bobFrame = bob.await(f -> isGame(f));
        JsonNode bobView = bobFrame.path("view");
        assertEquals("gin-rummy", bobFrame.path("game").asText());
        assertEquals(10, aliceView.path("myHand").size());
        assertEquals(10, bobView.path("myHand").size());
        assertEquals(31, bobView.path("stockCount").asInt());
        assertEquals(bob.playerId, bobView.path("onClock").asText(), "alice deals, so bob plays first");
        assertEquals(2, bobView.path("drawSources").size());
        assertEquals(0, aliceView.path("drawSources").size());

        bob.send("intent", Map.of("type", "draw", "source", "stock"));
        bob.await(f -> "accepted".equals(f.path("type").asText()));
        JsonNode discarding = bob.await(f -> isGame(f) && "DISCARD".equals(f.at("/view/phase").asText())).path("view");
        assertEquals(11, discarding.path("myHand").size());

        bob.send("intent", Map.of("type", "discard", "card", discarding.path("discards").get(0), "knock", false));
        bob.await(f -> "accepted".equals(f.path("type").asText()));
        JsonNode aliceTurn = alice.await(f -> isGame(f) && alice.playerId.equals(f.at("/view/onClock").asText()));
        assertEquals(discarding.path("discards").get(0), aliceTurn.at("/view/discardTop"));
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
        return createRoom("high-card");
    }

    private String createRoom(String gameId) {
        JsonNode created = http.postForObject("/api/rooms", Map.of("gameId", gameId), JsonNode.class);
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
