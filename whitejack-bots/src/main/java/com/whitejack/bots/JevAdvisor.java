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
 * TypeSafe's Jev, a decision model: it takes a {@code state} and typed questions and answers a
 * {@code choice} question with a probability for every option — exactly a bot's problem, so
 * there is no prose to parse. The options are the {@link Choice}'s legal moves, keyed "1".."n".
 *
 * <p>Served by OpenRouter's decisions endpoint (the default) and by TypeSafe's own
 * {@code https://api.typesafe.ai/v1/systemone} with model {@code jev-latest}; both take the
 * same body. Every failure is an empty answer.
 */
public final class JevAdvisor implements Advisor {

    public static final String DEFAULT_MODEL = "~typesafe/jev-latest";
    public static final String DEFAULT_ENDPOINT = "https://openrouter.ai/api/alpha/decisions";

    private static final System.Logger LOG = System.getLogger(JevAdvisor.class.getName());
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String QUESTION = "move";

    private final HttpClient http;
    private final URI endpoint;
    private final String apiKey;
    private final String model;
    private final Duration timeout;

    public JevAdvisor(URI endpoint, String apiKey, String model, Duration timeout) {
        this(HttpClient.newHttpClient(), endpoint, apiKey, model, timeout);
    }

    JevAdvisor(HttpClient http, URI endpoint, String apiKey, String model, Duration timeout) {
        this.http = Objects.requireNonNull(http, "http");
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model;
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    @Override
    public Optional<double[]> rank(String rules, Choice choice) {
        try {
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("X-Title", "whitejack")
                    .POST(HttpRequest.BodyPublishers.ofString(body(rules, choice)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                LOG.log(System.Logger.Level.WARNING, "Jev answered HTTP {0}: {1}", response.statusCode(),
                        abbreviate(response.body()));
                return Optional.empty();
            }
            JsonNode answer = JSON.readTree(response.body()).path("answers").path(QUESTION);
            Optional<double[]> ranking = ranking(answer, choice.options().size());
            ranking.ifPresent(r -> LOG.log(System.Logger.Level.DEBUG, "Jev chose {0} (confidence {1})",
                    answer.path("choice").asText(), answer.path("confidence").asDouble()));
            return ranking;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            LOG.log(System.Logger.Level.WARNING, "Jev request failed: {0}", e.toString());
            return Optional.empty();
        }
    }

    /** The per-option probabilities, or failing those a one-hot of the chosen option. */
    static Optional<double[]> ranking(JsonNode answer, int size) {
        double[] ranking = new double[size];
        JsonNode probabilities = answer.path("probabilities");
        boolean any = false;
        for (int i = 0; i < size; i++) {
            JsonNode p = probabilities.path(String.valueOf(i + 1));
            if (p.isNumber()) {
                ranking[i] = p.asDouble();
                any = true;
            }
        }
        if (any) return Optional.of(ranking);
        String chosen = answer.path("choice").asText("");
        for (int i = 0; i < size; i++) {
            if (chosen.equals(String.valueOf(i + 1))) {
                ranking[i] = 1;
                return Optional.of(ranking);
            }
        }
        return Optional.empty();
    }

    String body(String rules, Choice choice) {
        ObjectNode body = JSON.createObjectNode();
        body.put("model", model);
        body.putObject("state").put("game_rules", rules).put("position", choice.state());
        ObjectNode question = body.putObject("questions").putObject(QUESTION);
        question.put("type", "choice");
        question.put("instructions", choice.question());
        ObjectNode criteria = question.putObject("criteria");
        for (int i = 0; i < choice.options().size(); i++) {
            criteria.put(String.valueOf(i + 1), choice.options().get(i).label());
        }
        return body.toString();
    }

    private static String abbreviate(String text) {
        return text == null || text.length() <= 200 ? String.valueOf(text) : text.substring(0, 200) + "…";
    }
}
