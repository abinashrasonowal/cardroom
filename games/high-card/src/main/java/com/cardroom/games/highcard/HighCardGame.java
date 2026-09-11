package com.cardroom.games.highcard;

import com.cardroom.contract.Card;
import com.cardroom.contract.Deck;
import com.cardroom.contract.EngineContext;
import com.cardroom.contract.ErrorCode;
import com.cardroom.contract.GameDefinition;
import com.cardroom.contract.PlayerId;
import com.cardroom.contract.Seat;
import com.cardroom.contract.Turn;
import com.cardroom.contract.Validation;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Deal one card each in seat order, highest card wins. The whole game, so that the plumbing
 * around it has nowhere to hide.
 *
 * <p><b>House rule:</b> suit breaks a tied rank (spades high), which is what {@link Card}'s
 * natural order already does. That is one fewer branch than a tie list, and a tie in a game
 * this short is a worse demo than a winner.
 *
 * <p>Stateless by construction — no fields. One instance is shared by every room in the JVM.
 */
public final class HighCardGame implements GameDefinition<HcState, Draw, Drew> {

    private static final Duration TURN_LIMIT = Duration.ofSeconds(15);

    @Override
    public String id() {
        return "high-card";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public int minPlayers() {
        return 2;
    }

    @Override
    public int maxPlayers() {
        return 8;
    }

    @Override
    public Draw parseIntent(JsonNode raw) {
        if (raw == null || !"draw".equals(raw.path("type").asText())) {
            throw new IllegalArgumentException("high-card understands only {\"type\":\"draw\"}");
        }
        return new Draw();
    }

    @Override
    public HcState createInitialState(List<Seat> seats, Map<String, String> options, EngineContext ctx) {
        return new HcState(Deck.standard52().shuffled(ctx.random()), seats, Map.of());
    }

    @Override
    public Validation canJoin(HcState state, Seat seat) {
        if (!state.drawn().isEmpty()) {
            return Validation.reject(ErrorCode.SEAT_UNAVAILABLE, "the hand is already under way");
        }
        if (state.seats().size() >= maxPlayers()) {
            return Validation.reject(ErrorCode.SEAT_UNAVAILABLE, "table is full");
        }
        return Validation.OK;
    }

    @Override
    public Validation validate(HcState state, Draw intent, PlayerId actor) {
        if (state.allDrawn()) {
            return Validation.reject(ErrorCode.WRONG_PHASE, "every seat has drawn");
        }
        PlayerId onClock = state.seats().get(state.drawn().size()).id();
        if (!onClock.equals(actor)) {
            return Validation.reject(ErrorCode.NOT_YOUR_TURN, "waiting on " + onClock);
        }
        return Validation.OK;
    }

    @Override
    public List<Drew> reduce(HcState state, Draw intent, PlayerId actor, EngineContext ctx) {
        return List.of(new Drew(actor, state.deck().deal(1).card()));
    }

    @Override
    public HcState apply(HcState state, Drew event) {
        Deck.Deal deal = state.deck().deal(1);
        if (!deal.card().equals(event.card())) {
            // Replay diverged: this log was produced by a different deck than the one we hold.
            throw new IllegalStateException(
                    "event says " + event.card() + " but the deck's top card is " + deal.card());
        }
        Map<PlayerId, Card> drawn = new HashMap<>(state.drawn());
        drawn.put(event.player(), event.card());
        return new HcState(deal.rest(), state.seats(), drawn);
    }

    @Override
    public Optional<Turn> turn(HcState state) {
        if (state.allDrawn()) return Optional.empty();
        return Optional.of(new Turn(state.seats().get(state.drawn().size()).id(), TURN_LIMIT));
    }

    /** Drawing is always legal for whoever is on the clock, so the contract obligation holds. */
    @Override
    public Draw onTimeout(HcState state, PlayerId actor) {
        return new Draw();
    }

    @Override
    public HcView project(HcState state, Optional<PlayerId> viewer) {
        boolean revealed = isHandComplete(state);
        List<HcView.SeatView> seats = state.seats().stream()
                .map(seat -> new HcView.SeatView(
                        seat.index(),
                        seat.id(),
                        seat.nick(),
                        state.drawn().containsKey(seat.id()),
                        revealed || viewer.filter(seat.id()::equals).isPresent()
                                ? state.drawn().get(seat.id())
                                : null))
                .toList();
        return new HcView(
                seats,
                revealed,
                revealed ? winner(state) : null,
                turn(state).map(Turn::actor).orElse(null));
    }

    @Override
    public boolean isHandComplete(HcState state) {
        return state.allDrawn();
    }

    /**
     * Never true: a high-card room deals hand after hand until the host closes it. This is the
     * distinction {@code isHandComplete} cannot express on its own — the seed reveal happens
     * at every hand boundary, the room ends at none of them.
     */
    @Override
    public boolean isComplete(HcState state) {
        return false;
    }

    private static PlayerId winner(HcState state) {
        return state.drawn().entrySet().stream()
                .max(Map.Entry.comparingByValue(Comparator.naturalOrder()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
