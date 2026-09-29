package com.whitejack.bots;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.whitejack.bots.view.LobbyView;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A headless player speaking the public protocol (§14): it holds an ordinary signed token,
 * opens {@code /ws} like a browser tab, joins, and sends intents. There is no backdoor, so a
 * bot that can finish a game is also evidence that the protocol is complete.
 *
 * <p>Frames arrive on the WebSocket's thread and only record the newest view; deciding (which
 * may wait on the advisor) runs on {@code executor}, one decision at a time. A decision is sent
 * only if it still answers the newest view, so a slow model never plays a stale move.
 */
public final class Bot implements AutoCloseable {

    /**
     * @param ws the server's socket endpoint, e.g. {@code ws://127.0.0.1:8080/ws}
     * @param token a signed player token, as {@code /api/me} hands out
     * @param pace the least time between seeing a view and acting on it, so people can follow
     */
    public record Config(URI ws, String token, String playerId, String room, String nick, Advisor advisor,
            Duration pace, Executor executor) {
        public Config {
            Objects.requireNonNull(ws, "ws");
            Objects.requireNonNull(token, "token");
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(room, "room");
            Objects.requireNonNull(nick, "nick");
            Objects.requireNonNull(advisor, "advisor");
            Objects.requireNonNull(pace, "pace");
            Objects.requireNonNull(executor, "executor");
        }
    }

    /** The server refused to seat the bot. {@code error} is the wire {@code ErrorCode} name. */
    public static final class JoinRejected extends RuntimeException {
        private final String error;

        JoinRejected(String error, String detail) {
            super(error + ": " + detail);
            this.error = error;
        }

        public String error() {
            return error;
        }
    }

    private static final System.Logger LOG = System.getLogger(Bot.class.getName());
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final int MAX_RECONNECTS = 5;
    private static final int MAX_REJECTS = 2;

    private final Config config;
    private final String clientSeed;
    private final CompletableFuture<Bot> seated = new CompletableFuture<>();
    private final CompletableFuture<Void> finished = new CompletableFuture<>();
    private final AtomicBoolean thinking = new AtomicBoolean();
    private final AtomicBoolean closing = new AtomicBoolean();
    private final AtomicLong frameIds = new AtomicLong();
    private final AtomicLong viewVersion = new AtomicLong();

    private volatile WebSocket socket;
    private volatile String joinId;
    private volatile Seen latest;
    private volatile int reconnects;

    // Owned by the thinking thread.
    private long thoughtVersion = -1;
    private String sentKey;
    private String sentId;
    private String rejectedKey;
    private int rejects;

    /** The newest game view, with when it arrived. */
    private record Seen(long version, String gameId, JsonNode view, long atNanos) {}

    private Bot(Config config) {
        this.config = config;
        byte[] seed = new byte[16];
        new SecureRandom().nextBytes(seed);
        this.clientSeed = HexFormat.of().formatHex(seed);
    }

    /** Connects and joins; completes once the room lists the bot, or fails with {@link JoinRejected}. */
    public static CompletableFuture<Bot> join(Config config) {
        Bot bot = new Bot(config);
        bot.connect().exceptionally(e -> {
            bot.seated.completeExceptionally(e);
            bot.finished.complete(null);
            return null;
        });
        return bot.seated;
    }

    public String playerId() {
        return config.playerId();
    }

    public String nick() {
        return config.nick();
    }

    public String room() {
        return config.room();
    }

    /** Completes when the bot has left the table for good. */
    public CompletableFuture<Void> finished() {
        return finished;
    }

    /** Gives up the seat politely (frees it in the lobby) and disconnects. */
    public void leave() {
        if (closing.get()) return;
        try {
            send("leave", null);
        } catch (RuntimeException e) {
            // Leaving is best effort; closing below releases the socket either way.
        }
        close();
    }

    @Override
    public void close() {
        if (!closing.compareAndSet(false, true)) return;
        WebSocket ws = socket;
        if (ws != null) ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye").exceptionally(e -> null);
        seated.completeExceptionally(new IllegalStateException("bot closed before it was seated"));
        finished.complete(null);
    }

    private CompletableFuture<WebSocket> connect() {
        String query = "t=" + URLEncoder.encode(config.token(), StandardCharsets.UTF_8);
        URI uri = URI.create(config.ws() + (config.ws().getQuery() == null ? "?" : "&") + query);
        return HTTP.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(5)).buildAsync(uri, new Listener())
                .thenApply(ws -> {
                    socket = ws;
                    ObjectNode join = JSON.createObjectNode()
                            .put("room", config.room())
                            .put("nick", config.nick())
                            .put("clientSeed", clientSeed);
                    joinId = send("join", join);
                    return ws;
                });
    }

    private synchronized String send(String type, JsonNode payload) {
        String id = "b" + frameIds.incrementAndGet();
        ObjectNode frame = JSON.createObjectNode().put("v", 1).put("id", id).put("type", type);
        if (payload != null) frame.set("payload", payload);
        // The JDK socket forbids overlapping sends; waiting here serializes them.
        socket.sendText(frame.toString(), true).join();
        return id;
    }

    private void onFrame(JsonNode frame) {
        switch (frame.path("type").asText()) {
            case "update", "sync" -> onView(frame);
            case "rejected" -> onRejected(frame);
            case "fault" -> {
                LOG.log(System.Logger.Level.WARNING, "room {0} faulted: {1}", config.room(), frame.path("detail").asText());
                close();
            }
            default -> { /* accepted: nothing to do, the update that follows carries the result */ }
        }
    }

    private void onView(JsonNode frame) {
        JsonNode view = frame.path("view");
        if ("lobby".equals(frame.path("viewType").asText())) {
            LobbyView lobby = JSON.convertValue(view, LobbyView.class);
            boolean listed = lobby.members() != null
                    && lobby.members().stream().anyMatch(m -> m.id().equals(config.playerId()));
            if (listed) seated.complete(this);
            // Hosting means every person has gone: a table of bots should not linger.
            if (listed && config.playerId().equals(lobby.host()) && "LOBBY".equals(lobby.phase())) {
                config.executor().execute(this::leave);
            }
            return;
        }
        seated.complete(this); // a reconnect straight into a running game
        latest = new Seen(viewVersion.incrementAndGet(), frame.path("game").asText(), view, System.nanoTime());
        pump();
    }

    private void onRejected(JsonNode frame) {
        String error = frame.path("error").asText();
        String detail = frame.path("detail").asText();
        if (!seated.isDone()) {
            seated.completeExceptionally(new JoinRejected(error, detail));
            close();
            return;
        }
        String re = frame.path("re").asText(null);
        if (re == null || re.equals(joinId)) {
            // A re-join after a reconnect failed: the room is gone or has moved on without us.
            LOG.log(System.Logger.Level.INFO, "{0} could not rejoin {1} ({2})", config.nick(), config.room(), error);
            close();
            return;
        }
        if (re.equals(sentId)) {
            LOG.log(System.Logger.Level.WARNING, "{0} move rejected ({1}): {2}", config.nick(), error, detail);
            config.executor().execute(() -> {
                synchronized (this) {
                    rejectedKey = sentKey;
                    rejects++;
                    sentKey = null;
                    thoughtVersion = -1;
                }
                pump();
            });
        }
    }

    private void pump() {
        if (closing.get() || !thinking.compareAndSet(false, true)) return;
        config.executor().execute(() -> {
            try {
                think();
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.ERROR, "bot " + config.nick() + " failed to think", e);
            } finally {
                thinking.set(false);
            }
            // A view that landed while we were finishing would otherwise wait for the next one.
            Seen seen = latest;
            if (seen != null && seen.version() != thoughtVersionSnapshot()) pump();
        });
    }

    private synchronized long thoughtVersionSnapshot() {
        return thoughtVersion;
    }

    private void think() {
        while (!closing.get()) {
            Seen seen = latest;
            if (seen == null) return;
            synchronized (this) {
                if (seen.version() == thoughtVersion) return;
                thoughtVersion = seen.version();
            }
            Optional<Choice> choice = decide(seen);
            if (choice.isEmpty()) continue;
            Choice c = choice.get();
            String key;
            boolean fallbackOnly;
            synchronized (this) {
                if (c.key().equals(sentKey)) continue;
                if (c.key().equals(rejectedKey) && rejects >= MAX_REJECTS) continue; // let the clock play it
                fallbackOnly = c.key().equals(rejectedKey);
                key = c.key();
            }
            List<Integer> pick = fallbackOnly ? c.fallback() : pick(seen.gameId(), c);
            waitForPace(seen);
            if (latest != seen && !decide(latest).map(Choice::key).equals(Optional.of(key))) continue;
            synchronized (this) {
                sentKey = key;
                if (!key.equals(rejectedKey)) {
                    rejectedKey = null;
                    rejects = 0;
                }
                sentId = send("intent", c.intent(pick));
            }
        }
    }

    private Optional<Choice> decide(Seen seen) {
        Optional<GameBrain<?>> brain = Brains.forGame(seen.gameId());
        if (brain.isEmpty()) {
            LOG.log(System.Logger.Level.WARNING, "no bot brain for game {0}", seen.gameId());
            config.executor().execute(this::leave);
            return Optional.empty();
        }
        return decide(brain.get(), seen.view());
    }

    private <V> Optional<Choice> decide(GameBrain<V> brain, JsonNode raw) {
        V view;
        try {
            view = JSON.treeToValue(raw, brain.viewType());
        } catch (Exception e) {
            LOG.log(System.Logger.Level.WARNING, "unreadable {0} view: {1}", brain.gameId(), e.toString());
            return Optional.empty();
        }
        if (brain.isOver(view)) {
            config.executor().execute(this::leave);
            return Optional.empty();
        }
        return brain.choose(view, config.playerId());
    }

    private List<Integer> pick(String gameId, Choice choice) {
        if (choice.isForced()) return choice.fallback();
        String rules = Brains.forGame(gameId).map(GameBrain::rules).orElse("");
        Optional<String> reply = config.advisor().ask(Prompts.system(rules, choice.picks()), Prompts.user(choice));
        Optional<List<Integer>> parsed = reply.flatMap(text -> Prompts.parse(text, choice));
        if (reply.isPresent() && parsed.isEmpty()) {
            LOG.log(System.Logger.Level.INFO, "{0}: unusable advice {1}, playing the fallback", config.nick(), reply.get());
        }
        return parsed.orElse(choice.fallback());
    }

    private void waitForPace(Seen seen) {
        long left = config.pace().toNanos() - (System.nanoTime() - seen.atNanos());
        if (left <= 0) return;
        try {
            TimeUnit.NANOSECONDS.sleep(left);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void reconnect() {
        if (closing.get()) return;
        if (!seated.isDone() || ++reconnects > MAX_RECONNECTS) {
            close();
            return;
        }
        Executor later = CompletableFuture.delayedExecutor(reconnects, TimeUnit.SECONDS, config.executor());
        CompletableFuture.runAsync(() -> connect().exceptionally(e -> {
            reconnect();
            return null;
        }), later);
    }

    private final class Listener implements WebSocket.Listener {
        private final StringBuilder partial = new StringBuilder();

        @Override
        public void onOpen(WebSocket ws) {
            reconnects = 0;
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                String text = partial.toString();
                partial.setLength(0);
                try {
                    onFrame(JSON.readTree(text));
                } catch (Exception e) {
                    LOG.log(System.Logger.Level.WARNING, "bad frame from server: {0}", e.toString());
                }
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            reconnect();
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            LOG.log(System.Logger.Level.WARNING, "bot socket error: {0}", error.toString());
            reconnect();
        }
    }
}
