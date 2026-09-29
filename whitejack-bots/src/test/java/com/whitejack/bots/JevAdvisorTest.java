package com.whitejack.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Against a local stand-in for the OpenRouter endpoint. */
class JevAdvisorTest {

    private HttpServer server;

    private URI serve(int status, String body, AtomicReference<JsonNode> request, AtomicReference<String> auth)
            throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat", exchange -> {
            request.set(new ObjectMapper().readTree(exchange.getRequestBody()));
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/chat");
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    @Test
    void sendsJevTheConversationAndReturnsItsReply() throws Exception {
        AtomicReference<JsonNode> request = new AtomicReference<>();
        AtomicReference<String> auth = new AtomicReference<>();
        URI uri = serve(200, "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"MOVE: 2\"}}]}", request, auth);
        JevAdvisor jev = new JevAdvisor(HttpClient.newHttpClient(), uri, "sk-test", null, Duration.ofSeconds(5));

        assertEquals(Optional.of("MOVE: 2"), jev.ask("sys", "user"));
        assertEquals("Bearer sk-test", auth.get());
        assertEquals(JevAdvisor.DEFAULT_MODEL, request.get().path("model").asText());
        assertEquals("system", request.get().path("messages").get(0).path("role").asText());
        assertEquals("user", request.get().path("messages").get(1).path("content").asText());
    }

    @Test
    void anErrorStatusIsNoAdvice() throws Exception {
        URI uri = serve(503, "{\"error\":{\"message\":\"no endpoints\"}}", new AtomicReference<>(), new AtomicReference<>());
        JevAdvisor jev = new JevAdvisor(HttpClient.newHttpClient(), uri, "sk-test", "typesafe/jev-router", Duration.ofSeconds(5));
        assertTrue(jev.ask("sys", "user").isEmpty());
    }

    @Test
    void anUnreachableEndpointIsNoAdvice() {
        JevAdvisor jev = new JevAdvisor(HttpClient.newHttpClient(), URI.create("http://127.0.0.1:1/chat"), "k", null,
                Duration.ofSeconds(2));
        assertTrue(jev.ask("sys", "user").isEmpty());
    }
}
