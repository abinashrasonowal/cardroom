package com.whitejack.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.whitejack.bots.Advisor;
import com.whitejack.server.service.PlayerIdentityService;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * One person and bots at a real server: the bots join over the public socket, and a hand only
 * finishes if they keep answering with legal moves. The advisor is a stand-in that ranks the
 * last option highest, so the ranking-to-intent path runs without the network.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "whitejack.bots.pace=0ms")
class BotIntegrationTest {

    private static final String SEED = "0123456789abcdef";
    private static final Duration HAND = Duration.ofSeconds(60);

    @TestConfiguration
    static class FakeJev {
        static final AtomicInteger ASKED = new AtomicInteger();

        /** Ranks later options higher, so it disagrees with most fallbacks (which favour index 0). */
        @Bean
        @Primary
        Advisor fakeJev() {
            return (rules, choice) -> {
                ASKED.incrementAndGet();
                double[] ranking = new double[choice.options().size()];
                for (int i = 0; i < ranking.length; i++) ranking[i] = i;
                return Optional.of(ranking);
            };
        }
    }

    @LocalServerPort int port;
    @Autowired TestRestTemplate http;
    @Autowired PlayerIdentityService cookies;
    @Autowired ObjectMapper json;

    private Client human;

    @AfterEach
    void closeSocket() throws Exception {
        if (human != null) human.session.close();
    }

    @Test
    void threeBotsFinishAHandOfHeartsWithAPerson() throws Exception {
        String room = createRoom("hearts");
        human = seatHuman(room);
        for (int i = 0; i < 3; i++) addBot(room);
        human.await(frame -> isLobbyWith(frame, 4));
        int asked = FakeJev.ASKED.get();

        human.send("start", Map.of());
        human.playUntil(frame -> frame.at("/view/hand").asInt() >= 1 || !"PLAYING".equals(frame.at("/view/phase").asText())
                        && !"PASSING".equals(frame.at("/view/phase").asText()),
                this::heartsMove);
        assertTrue(FakeJev.ASKED.get() > asked, "the bots consulted the advisor");

        // Hand 0 is over, so its reasoning is released — labels and probabilities, never the bot's state.
        JsonNode notes = http.getForObject("/api/rooms/" + room + "/bot-notes", JsonNode.class);
        assertEquals(0, notes.path(0).path("hand").asInt(), notes.toString());
        JsonNode decisions = notes.path(0).path("decisions");
        assertTrue(decisions.size() > 0, "the bots' moves were recorded");
        for (JsonNode decision : decisions) {
            assertTrue(decision.path("nick").asText().endsWith("(bot)"));
            assertTrue(decision.path("state").isMissingNode(), "no private position text is served");
        }
        assertTrue(java.util.stream.StreamSupport.stream(decisions.spliterator(), false)
                .anyMatch(d -> "ADVISOR".equals(d.path("source").asText()) && d.at("/options/0/probability").isNumber()),
                "advisor probabilities are included");
    }

    @Test
    void aBotPlaysGinRummyTurnsBack() throws Exception {
        String room = createRoom("gin-rummy");
        human = seatHuman(room);
        addBot(room);
        human.await(frame -> isLobbyWith(frame, 2));

        human.send("start", Map.of());
        AtomicInteger myTurns = new AtomicInteger();
        human.playUntil(frame -> myTurns.get() >= 6, frame -> {
            Optional<ObjectNode> move = ginMove(frame);
            if (move.isPresent() && "draw".equals(move.get().path("type").asText())) myTurns.incrementAndGet();
            return move;
        });
    }

    @Test
    void botsDrawAtHighCard() throws Exception {
        String room = createRoom("high-card");
        human = seatHuman(room);
        for (int i = 0; i < 3; i++) addBot(room);
        human.await(frame -> isLobbyWith(frame, 4));

        human.send("start", Map.of());
        human.playUntil(frame -> frame.at("/view/handComplete").asBoolean(), frame ->
                human.playerId.equals(frame.at("/view/onClock").asText())
                        ? Optional.of(json.createObjectNode().put("type", "draw"))
                        : Optional.empty());
    }

    @Test
    void aFullTableTurnsTheBotAway() throws Exception {
        String room = createRoom("gin-rummy");
        human = seatHuman(room);
        addBot(room);
        ResponseEntity<JsonNode> refused = http.postForEntity("/api/rooms/" + room + "/bots", null, JsonNode.class);
        assertEquals(409, refused.getStatusCode().value());
        assertEquals("SEAT_UNAVAILABLE", refused.getBody().path("error").asText());
    }

    @Test
    void anUnknownRoomIsNotFound() {
        ResponseEntity<JsonNode> missing = http.postForEntity("/api/rooms/ZZZZZZ/bots", null, JsonNode.class);
        assertEquals(404, missing.getStatusCode().value());
        assertEquals("ROOM_NOT_FOUND", missing.getBody().path("error").asText());
    }

    // ---- the person's side: always the first legal option ----

    private Optional<ObjectNode> heartsMove(JsonNode frame) {
        JsonNode view = frame.path("view");
        JsonNode legal = view.path("legal");
        if (legal.isEmpty()) return Optional.empty();
        if ("PASSING".equals(view.path("phase").asText())) {
            ObjectNode pass = json.createObjectNode().put("type", "pass");
            ArrayNode cards = pass.putArray("cards");
            for (int i = 0; i < 3; i++) cards.add(legal.get(i));
            return Optional.of(pass);
        }
        if (!human.playerId.equals(view.path("onClock").asText())) return Optional.empty();
        ObjectNode play = json.createObjectNode().put("type", "play");
        play.set("card", legal.get(0));
        return Optional.of(play);
    }

    private Optional<ObjectNode> ginMove(JsonNode frame) {
        JsonNode view = frame.path("view");
        if (!human.playerId.equals(view.path("onClock").asText())) return Optional.empty();
        if (!view.path("drawSources").isEmpty()) {
            return Optional.of(json.createObjectNode().put("type", "draw").put("source", "stock"));
        }
        if (!view.path("discards").isEmpty()) {
            ObjectNode discard = json.createObjectNode().put("type", "discard").put("knock", false);
            discard.set("card", view.path("discards").get(0));
            return Optional.of(discard);
        }
        return Optional.empty();
    }

    // ---- helpers ----

    private String createRoom(String gameId) {
        return http.postForObject("/api/rooms", Map.of("gameId", gameId), JsonNode.class).path("room").asText();
    }

    private Client seatHuman(String room) throws Exception {
        Client client = connect(cookies.mint());
        client.send("join", Map.of("room", room, "nick", "ada", "clientSeed", SEED));
        client.await(frame -> isLobbyWith(frame, 1));
        return client;
    }

    private void addBot(String room) {
        ResponseEntity<JsonNode> added = http.postForEntity("/api/rooms/" + room + "/bots", null, JsonNode.class);
        assertEquals(200, added.getStatusCode().value(), String.valueOf(added.getBody()));
        assertTrue(added.getBody().path("nick").asText().endsWith("(bot)"));
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

        String send(String type, Object payload) throws Exception {
            String id = "c" + (++nextId);
            session.sendMessage(new TextMessage(json.writeValueAsString(
                    Map.of("v", 1, "id", id, "type", type, "payload", payload))));
            return id;
        }

        JsonNode await(Predicate<JsonNode> match) throws InterruptedException {
            return await(match, Duration.ofSeconds(5));
        }

        JsonNode await(Predicate<JsonNode> match, Duration wait) throws InterruptedException {
            long deadline = System.nanoTime() + wait.toNanos();
            while (true) {
                JsonNode frame = frames.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
                if (frame == null) throw new AssertionError("timed out waiting for a frame");
                if (match.test(frame)) return frame;
            }
        }

        /**
         * Plays {@code move} on every game view until {@code done}. After each move it waits for
         * the server's verdict, which also skips the views that were already stale when sent.
         */
        void playUntil(Predicate<JsonNode> done, Function<JsonNode, Optional<ObjectNode>> move) throws Exception {
            long deadline = System.nanoTime() + HAND.toNanos();
            while (true) {
                Duration left = Duration.ofNanos(deadline - System.nanoTime());
                if (left.isNegative()) throw new AssertionError("the hand did not finish in " + HAND);
                JsonNode frame = await(f -> "game".equals(f.path("viewType").asText()) || "fault".equals(f.path("type").asText()), left);
                if ("fault".equals(frame.path("type").asText())) throw new AssertionError("room faulted: " + frame);
                if (done.test(frame)) return;
                Optional<ObjectNode> mine = move.apply(frame);
                if (mine.isEmpty()) continue;
                String id = send("intent", mine.get());
                JsonNode verdict = await(f -> id.equals(f.path("re").asText()));
                assertEquals("accepted", verdict.path("type").asText(), verdict.toString());
            }
        }
    }
}
