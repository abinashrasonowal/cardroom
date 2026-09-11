package com.cardroom.contract;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The plug-in API. An implementation compiles against this module alone and is discovered at
 * runtime through {@code META-INF/services}.
 *
 * <p><b>Implementations must be stateless.</b> One instance is a {@code ServiceLoader}
 * singleton shared by every room thread in the JVM, so a single mutable field corrupts every
 * room at once — the likeliest bug a plug-in author will write. All state lives in {@code S},
 * which must be deeply immutable.
 *
 * @param <S> game state, deeply immutable
 * @param <I> this game's intents
 * @param <E> this game's events
 */
public interface GameDefinition<S, I extends Intent, E extends GameEvent> {

    String id();

    /** Stamped on every event. Replay of an old log needs to know which rules produced it. */
    int version();

    int minPlayers();

    int maxPlayers();

    /**
     * The JSON boundary. The gateway holds a {@code GameDefinition<?,?,?>} and cannot produce
     * an {@code I}; only the definition can, which is what keeps the generics honest.
     *
     * @throws IllegalArgumentException if the payload is not a move this game understands
     */
    I parseIntent(JsonNode raw);

    S createInitialState(List<Seat> seats, Map<String, String> options, EngineContext ctx);

    /** May someone sit down in this state? Blackjack and rummy disagree, so the game answers. */
    Validation canJoin(S state, Seat seat);

    Validation validate(S state, I intent, PlayerId actor);

    /** Pure. Time and randomness come from {@code ctx}, never from static calls. */
    List<E> reduce(S state, I intent, PlayerId actor, EngineContext ctx);

    /** Pure fold. Must not throw for an event this game produced. */
    S apply(S state, E event);

    /** {@code Optional.empty()} means nobody is on the clock. */
    Optional<Turn> turn(S state);

    /**
     * The move to play for someone who ran out of time.
     *
     * <p>Contract obligation: {@code validate(s, onTimeout(s, p), p)} must be {@code Ok}.
     * Otherwise the reject changes nothing, the clock re-arms, the same timeout fires, and the
     * room freezes alive. A contract test asserts this for every game.
     */
    I onTimeout(S state, PlayerId actor);

    /** {@code Optional.empty()} viewer = spectator. */
    PlayerView project(S state, Optional<PlayerId> viewer);

    boolean isHandComplete(S state);

    boolean isComplete(S state);
}
