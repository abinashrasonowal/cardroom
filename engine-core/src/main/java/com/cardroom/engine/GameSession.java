package com.cardroom.engine;

import com.cardroom.contract.EngineContext;
import com.cardroom.contract.ErrorCode;
import com.cardroom.contract.GameDefinition;
import com.cardroom.contract.GameEvent;
import com.cardroom.contract.Intent;
import com.cardroom.contract.PlayerId;
import com.cardroom.contract.PlayerView;
import com.cardroom.contract.Seat;
import com.cardroom.contract.Turn;
import com.cardroom.contract.Validation;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One game in progress: the validate → reduce → fold → append loop of §9, and nothing else.
 * Broadcasting and the turn clock belong to the room actor, which is why this class has no
 * ports and needs no I/O to test.
 *
 * <p><b>Threading.</b> {@link #submit} runs only on the room's single thread. {@code state} is
 * volatile and every {@code S} is deeply immutable, so {@link #view} and {@link #log} are safe
 * to read from another thread.
 */
public final class GameSession<S, I extends Intent, E extends GameEvent> {

    /**
     * A game module looping in {@code reduce} cannot be preempted (§10), and an unbounded event
     * list is the cheap half of that failure — it exhausts heap before anyone notices. The
     * mitigation is this cap, stated honestly, not a sandbox.
     */
    private static final int MAX_EVENTS_PER_INTENT = 256;

    private final GameDefinition<S, I, E> def;
    private final EngineContext ctx;
    private final EventLog<E> log = new EventLog<>();

    private volatile S state;

    private GameSession(GameDefinition<S, I, E> def, EngineContext ctx, S initial) {
        this.def = def;
        this.ctx = ctx;
        this.state = initial;
    }

    /**
     * Captures the wildcards from {@code GameCatalog.require(id)}: the caller holds a
     * {@code GameDefinition<?,?,?>} and cannot name {@code S}, {@code I} or {@code E}, but
     * javac can, which is the whole reason this is a static factory.
     */
    public static <S, I extends Intent, E extends GameEvent> GameSession<S, I, E> start(
            GameDefinition<S, I, E> def, List<Seat> seats, Map<String, String> options, EngineContext ctx) {
        S initial = Objects.requireNonNull(
                def.createInitialState(seats, options, ctx), "createInitialState returned null");
        return new GameSession<>(def, ctx, initial);
    }

    /**
     * The loop. A rejection is a total no-op: no event, no sequence bump, no state change, and
     * no other player learns the attempt was made.
     */
    public Outcome submit(JsonNode raw, PlayerId actor) {
        I intent;
        try {
            intent = def.parseIntent(raw);
        } catch (IllegalArgumentException e) {
            // Client input, not a server fault: a malformed frame must cost that caller an
            // error message, never the room. Any other RuntimeException is a game bug and is
            // left to propagate so the actor closes the room loudly (§10).
            String detail = e.getMessage() == null ? "unparseable intent" : e.getMessage();
            return new Outcome(log.seq(), Validation.reject(ErrorCode.MALFORMED_INTENT, detail));
        }

        return run(intent, actor);
    }

    /**
     * The turn clock's path in. {@code onTimeout} returns an {@code I} directly, so there is no
     * JSON to parse — but everything after parsing is identical, and sharing it is what stops
     * the timeout path from drifting away from the player path.
     */
    public Outcome submitTimeout(PlayerId actor) {
        return run(def.onTimeout(state, actor), actor);
    }

    private Outcome run(I intent, PlayerId actor) {
        Validation verdict = def.validate(state, intent, actor);
        if (!verdict.isOk()) {
            return new Outcome(log.seq(), verdict);
        }

        List<E> events = Objects.requireNonNull(
                def.reduce(state, intent, actor, ctx), "reduce returned null");
        if (events.size() > MAX_EVENTS_PER_INTENT) {
            throw new IllegalStateException(
                    def.id() + " produced " + events.size() + " events for one intent (cap "
                            + MAX_EVENTS_PER_INTENT + ")");
        }

        // Fold FIRST, into a local. Appending first would leave a log the state does not
        // reflect the moment apply throws on event 3 of 5, and replay would diverge forever.
        S next = state;
        for (E event : events) {
            next = Objects.requireNonNull(def.apply(next, event), "apply returned null");
        }

        long at = ctx.now();
        for (E event : events) {
            log.append(event, at, def.version());
        }
        state = next; // swap only once every event has folded cleanly

        return new Outcome(log.seq(), Validation.OK);
    }

    public PlayerView view(Optional<PlayerId> viewer) {
        return def.project(state, viewer);
    }

    public Optional<Turn> turn() {
        return def.turn(state);
    }

    public I onTimeout(PlayerId actor) {
        return def.onTimeout(state, actor);
    }

    public Validation canJoin(Seat seat) {
        return def.canJoin(state, seat);
    }

    public boolean isHandComplete() {
        return def.isHandComplete(state);
    }

    public boolean isComplete() {
        return def.isComplete(state);
    }

    public long seq() {
        return log.seq();
    }

    public String gameId() {
        return def.id();
    }

    public S state() {
        return state;
    }

    public EventLog<E> log() {
        return log;
    }
}
