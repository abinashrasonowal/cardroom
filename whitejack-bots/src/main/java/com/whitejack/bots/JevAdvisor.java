package com.whitejack.bots;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * TypeSafe's Jev on OpenRouter, through the OpenAI-compatible chat completions endpoint.
 *
 * <p>Jev lists no tool-calling or JSON-mode parameters, so the reply is plain text and
 * {@link Prompts#parse} reads the move out of it. Every failure is an empty answer.
 */
public final class JevAdvisor implements Advisor {

    public static final String DEFAULT_MODEL = "typesafe/jev-router";
    public static final URI OPENROUTER = URI.create("https://openrouter.ai/api/v1/chat/completions");

    private static final System.Logger LOG = System.getLogger(JevAdvisor.class.getName());
    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http;
    private final URI endpoint;
    private final String apiKey;
    private final String model;
    private final Duration timeout;

    public JevAdvisor(String apiKey, String model, Duration timeout) {
        this(HttpClient.newHttpClient(), OPENROUTER, apiKey, model, timeout);
    }

    JevAdvisor(HttpClient http, URI endpoint, String apiKey, String model, Duration timeout) {
        this.http = Objects.requireNonNull(http, "http");
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model;
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    @Override
    public Optional<String> ask(String system, String user) {
        try {
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("X-Title", "whitejack")
                    .POST(HttpRequest.BodyPublishers.ofString(body(system, user)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                LOG.log(System.Logger.Level.WARNING, "Jev answered HTTP {0}: {1}", response.statusCode(),
                        abbreviate(response.body()));
                return Optional.empty();
            }
            JsonNode content = JSON.readTree(response.body()).path("choices").path(0).path("message").path("content");
            return content.isTextual() ? Optional.of(content.asText()) : Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            LOG.log(System.Logger.Level.WARNING, "Jev request failed: {0}", e.toString());
            return Optional.empty();
        }
    }

    private String body(String system, String user) {
        ObjectNode body = JSON.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0.2);
        body.put("max_tokens", 200);
        body.putArray("messages")
                .add(JSON.createObjectNode().put("role", "system").put("content", system))
                .add(JSON.createObjectNode().put("role", "user").put("content", user));
        return body.toString();
    }

    private static String abbreviate(String text) {
        return text == null || text.length() <= 200 ? String.valueOf(text) : text.substring(0, 200) + "…";
    }
}
