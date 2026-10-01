package com.whitejack.bots;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Against a local stand-in for the decisions endpoint, answering the way Jev does. */
class JevAdvisorTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpServer server;

    private static Choice choice(int picks, String... labels) {
        List<Move> moves = java.util.Arrays.stream(labels)
                .map(label -> new Move(label, JsonNodeFactory.instance.textNode(label)))
                .toList();
        List<Integer> fallback = java.util.stream.IntStream.range(0, picks).boxed().toList();
        return new Choice("k", "Your cards: 3♣ K♣", "Which card?", moves, picks, fallback, picked -> picked.get(0).value());
    }

    private URI serve(int status, String body, AtomicReference<JsonNode> request, AtomicReference<String> auth)
            throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/decisions", exchange -> {
            request.set(JSON.readTree(exchange.getRequestBody()));
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/decisions");
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    @Test
    void asksAChoiceQuestionWhoseOptionsAreTheLegalMoves() throws Exception {
        AtomicReference<JsonNode> request = new AtomicReference<>();
        AtomicReference<String> auth = new AtomicReference<>();
        URI uri = serve(200, "{\"model\":\"typesafe/jev-1.13-20260917\",\"answers\":{\"move\":{\"type\":\"choice\","
                + "\"choice\":\"2\",\"probabilities\":{\"1\":0.2,\"2\":0.7,\"3\":0.1},\"confidence\":0.5}}}", request, auth);
        JevAdvisor jev = new JevAdvisor(HttpClient.newHttpClient(), uri, "sk-test", null, Duration.ofSeconds(5));

        double[] ranking = jev.rank("Hearts.", choice(1, "3♣", "K♣", "Q♠")).orElseThrow();

        assertArrayEquals(new double[] {0.2, 0.7, 0.1}, ranking);
        assertEquals("Bearer sk-test", auth.get());
        JsonNode sent = request.get();
        assertEquals(JevAdvisor.DEFAULT_MODEL, sent.path("model").asText());
        assertEquals("Hearts.", sent.at("/state/game_rules").asText());
        assertEquals("choice", sent.at("/questions/move/type").asText());
        assertEquals("Which card?", sent.at("/questions/move/instructions").asText());
        assertEquals("K♣", sent.at("/questions/move/criteria/2").asText());
        assertEquals(3, sent.at("/questions/move/criteria").size());
    }

    @Test
    void theRankingPicksTheBestSeveralForAMultiCardPass() {
        Choice pass = choice(3, "2♣", "Q♠", "A♥", "5♦", "K♠");
        assertEquals(List.of(2, 4, 1), pass.best(new double[] {0.01, 0.1, 0.6, 0.04, 0.25}));
    }

    @Test
    void aBareChoiceWithoutProbabilitiesStillRanks() throws Exception {
        JsonNode answer = JSON.readTree("{\"type\":\"choice\",\"choice\":\"3\"}");
        assertArrayEquals(new double[] {0, 0, 1}, JevAdvisor.ranking(answer, 3).orElseThrow());
        assertTrue(JevAdvisor.ranking(JSON.readTree("{\"choice\":\"9\"}"), 3).isEmpty(), "not one of the options");
    }

    @Test
    void anErrorStatusIsNoAdvice() throws Exception {
        URI uri = serve(429, "{\"error\":{\"message\":\"rate limited\"}}", new AtomicReference<>(), new AtomicReference<>());
        JevAdvisor jev = new JevAdvisor(HttpClient.newHttpClient(), uri, "sk-test", "jev-latest", Duration.ofSeconds(5));
        assertTrue(jev.rank("", choice(1, "a", "b")).isEmpty());
    }

    @Test
    void anUnreachableEndpointIsNoAdvice() {
        JevAdvisor jev = new JevAdvisor(HttpClient.newHttpClient(), URI.create("http://127.0.0.1:1/decisions"), "k", null,
                Duration.ofSeconds(2));
        assertTrue(jev.rank("", choice(1, "a", "b")).isEmpty());
    }
}
