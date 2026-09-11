package com.cardroom.games.highcard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cardroom.contract.Card;
import com.cardroom.contract.EngineContext;
import com.cardroom.contract.ErrorCode;
import com.cardroom.contract.PlayerId;
import com.cardroom.contract.RandomSource;
import com.cardroom.contract.Rank;
import com.cardroom.contract.Seat;
import com.cardroom.contract.Suit;
import com.cardroom.contract.Validation;
import com.cardroom.contract.Validation.Reject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HighCardGameTest {

    private static final HighCardGame GAME = new HighCardGame();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final PlayerId P0 = new PlayerId("p0");
    private static final PlayerId P1 = new PlayerId("p1");
    private static final PlayerId P2 = new PlayerId("p2");
    private static final List<Seat> SEATS =
            List.of(new Seat(0, P0, "abi"), new Seat(1, P1, "sam"), new Seat(2, P2, "kim"));

    /** Stands in for HmacRandom, which lives in engine-core and must never be on this classpath. */
    private static EngineContext ctx(long seed) {
        long[] s = {seed};
        RandomSource rng = bound -> {
            s[0] = s[0] * 6364136223846793005L + 1442695040888963407L;
            return (int) Long.remainderUnsigned(s[0] >>> 33, bound);
        };
        return new EngineContext() {
            @Override
            public long now() {
                return 1_700_000_000_000L;
            }

            @Override
            public RandomSource random() {
                return rng;
            }

            @Override
            public List<Seat> seats() {
                return SEATS;
            }
        };
    }

    /** The engine's loop from §9, minus the log: validate, reduce, fold. */
    private static HcState draw(HcState state, PlayerId actor) {
        assertTrue(GAME.validate(state, new Draw(), actor).isOk(), "rejected a legal draw");
        HcState next = state;
        for (Drew event : GAME.reduce(state, new Draw(), actor, ctx(1))) {
            next = GAME.apply(next, event);
        }
        return next;
    }

    private static HcState playFullHand(HcState state) {
        for (Seat seat : SEATS) state = draw(state, seat.id());
        return state;
    }

    private static HcState fresh(long seed) {
        return GAME.createInitialState(SEATS, Map.of(), ctx(seed));
    }

    /** Every card anywhere in the serialized view, found structurally rather than by substring. */
    private static Set<Card> cardsIn(Object view) {
        Set<Card> found = new HashSet<>();
        collect(MAPPER.valueToTree(view), found);
        return found;
    }

    private static void collect(JsonNode node, Set<Card> out) {
        if (node.isObject() && node.has("rank") && node.has("suit")) {
            out.add(new Card(Rank.valueOf(node.get("rank").asText()), Suit.valueOf(node.get("suit").asText())));
            return;
        }
        node.forEach(child -> collect(child, out));
    }

    // ---- turn order and legality ----

    @Test
    void seatsDrawInOrderThenTheClockStops() {
        HcState state = fresh(7);
        assertEquals(P0, GAME.turn(state).orElseThrow().actor());
        state = draw(state, P0);
        assertEquals(P1, GAME.turn(state).orElseThrow().actor());
        state = draw(state, P1);
        assertEquals(P2, GAME.turn(state).orElseThrow().actor());
        state = draw(state, P2);
        assertTrue(GAME.turn(state).isEmpty());
        assertTrue(GAME.isHandComplete(state));
        assertFalse(GAME.isComplete(state), "the room deals another hand; only the hand ended");
    }

    @Test
    void drawingOutOfTurnIsRejected() {
        Validation v = GAME.validate(fresh(7), new Draw(), P2);
        assertEquals(ErrorCode.NOT_YOUR_TURN, ((Reject) v).code());
    }

    @Test
    void drawingAfterEverySeatHasDrawnIsRejected() {
        Validation v = GAME.validate(playFullHand(fresh(7)), new Draw(), P0);
        assertEquals(ErrorCode.WRONG_PHASE, ((Reject) v).code());
    }

    @Test
    void parseIntentAcceptsOnlyDraw() {
        assertEquals(new Draw(), GAME.parseIntent(MAPPER.createObjectNode().put("type", "draw")));
        assertThrows(IllegalArgumentException.class,
                () -> GAME.parseIntent(MAPPER.createObjectNode().put("type", "hit")));
        assertThrows(IllegalArgumentException.class, () -> GAME.parseIntent(MAPPER.createObjectNode()));
        assertThrows(IllegalArgumentException.class, () -> GAME.parseIntent(null));
    }

    /**
     * The contract obligation from §8. If this fails the room freezes alive: the reject changes
     * nothing, the clock re-arms, the same timeout fires again forever.
     */
    @Test
    void onTimeoutAlwaysReturnsAnIntentValidateAccepts() {
        HcState state = fresh(7);
        for (Seat seat : SEATS) {
            PlayerId onClock = GAME.turn(state).orElseThrow().actor();
            assertTrue(GAME.validate(state, GAME.onTimeout(state, onClock), onClock).isOk());
            state = draw(state, onClock);
        }
    }

    @Test
    void canJoinClosesOnceTheFirstCardIsDrawn() {
        Seat latecomer = new Seat(3, new PlayerId("p3"), "joe");
        assertTrue(GAME.canJoin(fresh(7), latecomer).isOk());
        Validation v = GAME.canJoin(draw(fresh(7), P0), latecomer);
        assertEquals(ErrorCode.SEAT_UNAVAILABLE, ((Reject) v).code());
    }

    // ---- determinism ----

    @Test
    void theSameSeedDealsTheSameHand() {
        assertEquals(playFullHand(fresh(99)).drawn(), playFullHand(fresh(99)).drawn());
        assertNotEquals(playFullHand(fresh(99)).drawn(), playFullHand(fresh(100)).drawn());
    }

    @Test
    void applyRefusesAnEventFromADifferentDeck() {
        HcState state = fresh(7);
        Card notTheTopCard = new Card(Rank.ACE, Suit.SPADES).equals(state.deck().cards().get(0))
                ? new Card(Rank.TWO, Suit.CLUBS)
                : new Card(Rank.ACE, Suit.SPADES);
        assertThrows(IllegalStateException.class, () -> GAME.apply(state, new Drew(P0, notTheTopCard)));
    }

    @Test
    void everyDealtCardLeavesTheDeck() {
        HcState state = playFullHand(fresh(7));
        assertEquals(49, state.deck().size());
        assertEquals(3, new HashSet<>(state.drawn().values()).size(), "the same card was dealt twice");
        for (Card dealt : state.drawn().values()) {
            assertFalse(state.deck().cards().contains(dealt));
        }
    }

    @Test
    void theHighestCardWins() {
        HcState state = playFullHand(fresh(7));
        PlayerId expected = state.drawn().entrySet().stream()
                .max(Map.Entry.comparingByValue(Comparator.naturalOrder()))
                .orElseThrow()
                .getKey();
        assertEquals(expected, GAME.project(state, Optional.of(P0)).winner());
    }

    // ---- redaction: the claim the whole project rests on ----

    @Test
    void aPlayerSeesOnlyTheirOwnCard() {
        HcState state = draw(draw(fresh(7), P0), P1);
        HcView view = GAME.project(state, Optional.of(P0));

        assertEquals(Set.of(state.drawn().get(P0)), cardsIn(view));
        assertTrue(view.seats().get(1).hasDrawn(), "that P1 has drawn is public; the card is not");
        assertNull(view.seats().get(1).card());
        assertNull(view.winner());
    }

    @Test
    void aSpectatorSeesNoCardsUntilTheReveal() {
        HcState state = draw(draw(fresh(7), P0), P1);
        assertEquals(Set.of(), cardsIn(GAME.project(state, Optional.empty())));
    }

    @Test
    void theRevealShowsEveryCardAndNothingElse() {
        HcState state = playFullHand(fresh(7));
        Set<Card> seen = cardsIn(GAME.project(state, Optional.of(P0)));

        assertEquals(new HashSet<>(state.drawn().values()), seen);
        assertEquals(3, seen.size());
        for (Card undealt : state.deck().cards()) {
            assertFalse(seen.contains(undealt), "the undealt deck leaked into the view");
        }
    }

    @Test
    void noViewerEverSeesTheUndealtDeck() {
        List<Optional<PlayerId>> viewers =
                new ArrayList<>(List.of(Optional.empty(), Optional.of(P0), Optional.of(P1), Optional.of(P2)));
        HcState state = fresh(7);
        for (int drawn = 0; drawn <= SEATS.size(); drawn++) {
            for (Optional<PlayerId> viewer : viewers) {
                for (Card undealt : state.deck().cards()) {
                    assertFalse(cardsIn(GAME.project(state, viewer)).contains(undealt),
                            "viewer " + viewer + " saw undealt " + undealt);
                }
            }
            if (drawn < SEATS.size()) state = draw(state, SEATS.get(drawn).id());
        }
    }
}
