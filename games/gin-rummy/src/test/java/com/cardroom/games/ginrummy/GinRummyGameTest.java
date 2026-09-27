package com.cardroom.games.ginrummy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cardroom.contract.Card;
import com.cardroom.contract.Deck;
import com.cardroom.contract.EngineContext;
import com.cardroom.contract.ErrorCode;
import com.cardroom.contract.PlayerId;
import com.cardroom.contract.RandomSource;
import com.cardroom.contract.Rank;
import com.cardroom.contract.Seat;
import com.cardroom.contract.Suit;
import com.cardroom.contract.Validation;
import com.cardroom.games.ginrummy.GinIntent.Discard;
import com.cardroom.games.ginrummy.GinIntent.Draw;
import com.cardroom.games.ginrummy.HandResult.Outcome;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GinRummyGameTest {

    private static final GinRummyGame GAME = new GinRummyGame();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final PlayerId P0 = new PlayerId("p0");
    private static final PlayerId P1 = new PlayerId("p1");
    private static final List<Seat> SEATS = List.of(new Seat(0, P0, "abi"), new Seat(1, P1, "sam"));

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
    private static GinState step(GinState state, GinIntent intent, PlayerId actor, EngineContext ctx) {
        Validation verdict = GAME.validate(state, intent, actor);
        assertTrue(verdict.isOk(), () -> actor + " " + intent + " rejected: " + verdict);
        GinState next = state;
        for (GinEvent event : GAME.reduce(state, intent, actor, ctx)) next = GAME.apply(next, event);
        return next;
    }

    private static ErrorCode rejection(GinState state, GinIntent intent, PlayerId actor) {
        return assertInstanceOf(Validation.Reject.class, GAME.validate(state, intent, actor)).code();
    }

    /** "QS", "10H", "AC". */
    private static Card c(String code) {
        String rank = code.substring(0, code.length() - 1);
        Suit suit = switch (code.charAt(code.length() - 1)) {
            case 'C' -> Suit.CLUBS;
            case 'D' -> Suit.DIAMONDS;
            case 'H' -> Suit.HEARTS;
            default -> Suit.SPADES;
        };
        return new Card(Arrays.stream(Rank.values()).filter(r -> r.symbol().equals(rank)).findFirst().orElseThrow(), suit);
    }

    private static List<Card> cards(String... codes) {
        return Arrays.stream(codes).map(GinRummyGameTest::c).toList();
    }

    /** P0 to discard, holding 11 cards; the stock and pile are made of cards nobody holds. */
    private static GinState discarding(List<Card> p0, List<Card> p1, int stockSize, Map<PlayerId, Integer> scores) {
        List<Card> rest = new ArrayList<>(Deck.standard52().cards());
        rest.removeAll(p0);
        rest.removeAll(p1);
        return new GinState(SEATS, 0, P1, GinPhase.DISCARD, P0, Map.of(P0, p0, P1, p1), rest.subList(1, 1 + stockSize),
                List.of(rest.get(0)), null, scores, List.of(), null);
    }

    private static GinState discarding(List<Card> p0, List<Card> p1) {
        return discarding(p0, p1, 20, Map.of(P0, 0, P1, 0));
    }

    // ---- melds ----

    @Test
    void setsAndRunsAreMeldedAndTheRestIsDeadwood() {
        Melds.Arrangement best = Melds.best(cards("7S", "7H", "7D", "3C", "4C", "5C", "KD"));
        assertEquals(2, best.melds().size());
        assertEquals(List.of(c("KD")), best.deadwood());
        assertEquals(10, best.deadwoodPoints());
    }

    @Test
    void acesAreLowInRunsAndDoNotWrapAroundTheKing() {
        assertEquals(0, Melds.best(cards("AC", "2C", "3C")).deadwoodPoints());
        assertEquals(21, Melds.best(cards("QS", "KS", "AS")).deadwoodPoints());
    }

    @Test
    void aCardShiftsToWhicheverMeldLeavesLessDeadwood() {
        // The 7♠ could complete either the set of sevens or the 5-6-7♠ run.
        assertEquals(11, Melds.best(cards("7S", "7H", "7D", "5S", "6S")).deadwoodPoints(), "set of 7s beats the run");
        assertEquals(0, Melds.best(cards("7S", "7H", "7D", "7C", "5S", "6S")).deadwoodPoints(), "three 7s plus the run");
    }

    @Test
    void deadwoodLaysOffOntoSetsAndRunEnds() {
        List<List<Card>> knocker = List.of(cards("7H", "7D", "7C"), cards("4S", "5S", "6S"));
        List<Card> laid = Melds.layoffs(cards("3S", "7S", "KD"), knocker);
        assertTrue(laid.containsAll(cards("3S", "7S")));
        assertFalse(laid.contains(c("KD")));
    }

    // ---- turns ----

    @Test
    void dealsTenEachTurnsOneUpAndTheNonDealerGoesFirst() {
        GinState state = GAME.createInitialState(SEATS, Map.of(), ctx(3));
        assertEquals(10, state.handOf(P0).size());
        assertEquals(10, state.handOf(P1).size());
        assertEquals(1, state.discard().size());
        assertEquals(31, state.stock().size());
        assertEquals(P0, state.dealer());
        assertEquals(P1, state.turn());
        assertEquals(GinPhase.DRAW, state.phase());
    }

    @Test
    void aTurnIsDrawThenDiscardAndPassesToTheOpponent() {
        GinState state = GAME.createInitialState(SEATS, Map.of(), ctx(3));
        assertEquals(ErrorCode.NOT_YOUR_TURN, rejection(state, new Draw(DrawSource.STOCK), P0));
        assertEquals(ErrorCode.WRONG_PHASE, rejection(state, new Discard(state.handOf(P1).get(0), false), P1));

        Card top = state.stock().get(0);
        state = step(state, new Draw(DrawSource.STOCK), P1, ctx(1));
        assertTrue(state.handOf(P1).contains(top));
        assertEquals(GinPhase.DISCARD, state.phase());
        assertEquals(ErrorCode.WRONG_PHASE, rejection(state, new Draw(DrawSource.STOCK), P1));

        state = step(state, new Discard(top, false), P1, ctx(1));
        assertEquals(top, state.discardTop());
        assertEquals(P0, state.turn());
        assertEquals(GinPhase.DRAW, state.phase());
    }

    @Test
    void theCardTakenFromThePileCannotGoStraightBack() {
        GinState state = GAME.createInitialState(SEATS, Map.of(), ctx(4));
        Card upcard = state.discardTop();
        state = step(state, new Draw(DrawSource.DISCARD), P1, ctx(1));
        assertEquals(ErrorCode.ILLEGAL_MOVE, rejection(state, new Discard(upcard, false), P1));
        assertFalse(GAME.project(state, Optional.of(P1)).discards().contains(upcard));
    }

    @Test
    void knockingNeedsTenOrLessDeadwood() {
        List<Card> opponent = cards("5D", "6D", "7D", "10C", "JC", "QD", "KC", "9H", "3H", "4C");
        GinState exactlyTen = discarding(cards("AC", "2C", "3C", "4D", "4H", "4S", "7S", "8S", "9S", "QH", "KD"), opponent);
        assertTrue(GAME.validate(exactlyTen, new Discard(c("KD"), true), P0).isOk(), "Q♥ left over: 10 may knock");

        GinState tooMuch = discarding(cards("AC", "2C", "3C", "4D", "4H", "4S", "7S", "8S", "QH", "KD", "5H"), opponent);
        assertEquals(ErrorCode.ILLEGAL_MOVE, rejection(tooMuch, new Discard(c("5H"), true), P0), "35 deadwood");
        assertTrue(GAME.validate(tooMuch, new Discard(c("5H"), false), P0).isOk(), "discarding without knocking is fine");
    }

    // ---- scoring ----

    @Test
    void aKnockScoresTheDeadwoodDifferenceAfterLayoffs() {
        GinState state = discarding(cards("AC", "2C", "3C", "4D", "4H", "4S", "7S", "8S", "9S", "2H", "KD"),
                cards("5D", "6D", "7D", "10C", "JC", "QD", "KC", "9H", "3H", "4C"));
        state = step(state, new Discard(c("KD"), true), P0, ctx(1));

        HandResult result = state.lastHand();
        assertEquals(Outcome.KNOCK, result.outcome());
        assertEquals(P0, result.winner());
        assertTrue(result.hands().get(1).laidOff().contains(c("4C")), "4♣ extends the knocker's A-2-3♣");
        assertEquals(52 - 2, result.points(), "defender 52 after laying off, knocker 2");
        assertEquals(List.of(50, 0), state.history().get(0));
        assertEquals(1, state.hand(), "the knock deals the next hand");
        assertEquals(P0, state.dealer(), "the deal alternates: P1 dealt this hand, P0 deals the next");
        assertEquals(P1, state.turn(), "the non-dealer leads");
    }

    @Test
    void ginScoresTwentyFivePlusTheDefendersDeadwoodWithNoLayoffs() {
        GinState state = discarding(cards("AC", "2C", "3C", "4D", "4H", "4S", "7S", "8S", "9S", "10S", "KD"),
                cards("5D", "6D", "7D", "10C", "JC", "QD", "KC", "9H", "3H", "4C"));
        state = step(state, new Discard(c("KD"), true), P0, ctx(1));
        assertEquals(Outcome.GIN, state.lastHand().outcome());
        assertTrue(state.lastHand().hands().get(1).laidOff().isEmpty());
        assertEquals(25 + 56, state.lastHand().points());
    }

    @Test
    void aDefenderWithNoMoreDeadwoodUndercutsTheKnocker() {
        GinState state = discarding(cards("AC", "2C", "3C", "4D", "4H", "4S", "7S", "8S", "9S", "9H", "KD"),
                cards("5D", "6D", "7D", "10C", "JC", "QC", "2S", "2H", "2D", "3H"));
        state = step(state, new Discard(c("KD"), true), P0, ctx(1));
        assertEquals(Outcome.UNDERCUT, state.lastHand().outcome());
        assertEquals(P1, state.lastHand().winner());
        assertEquals(25 + (9 - 3), state.lastHand().points());
    }

    @Test
    void aHandIsDeadWhenTheStockRunsDownToTwo() {
        GinState state = discarding(cards("AC", "2C", "3C", "4D", "4H", "4S", "7S", "8S", "9S", "QH", "KD"),
                cards("5D", "6D", "7D", "10C", "JC", "QD", "KC", "9H", "3H", "4C"), 2, Map.of(P0, 0, P1, 0));
        state = step(state, new Discard(c("KD"), false), P0, ctx(1));
        assertEquals(Outcome.DEAD, state.lastHand().outcome());
        assertEquals(List.of(0, 0), state.history().get(0));
        assertEquals(1, state.hand());
        assertEquals(31, state.stock().size(), "redealt");
    }

    @Test
    void reachingOneHundredEndsTheGame() {
        GinState state = discarding(cards("AC", "2C", "3C", "4D", "4H", "4S", "7S", "8S", "9S", "2H", "KD"),
                cards("5D", "6D", "7D", "10C", "JC", "QD", "KC", "9H", "3H", "4C"), 20, Map.of(P0, 60, P1, 30));
        state = step(state, new Discard(c("KD"), true), P0, ctx(1));
        assertEquals(GinPhase.GAME_OVER, state.phase());
        assertEquals(110, state.scores().get(P0));
        assertTrue(GAME.isComplete(state));
        assertEquals(Optional.empty(), GAME.turn(state));
        assertEquals(P0, GAME.project(state, Optional.of(P1)).winner());
    }

    // ---- boundary ----

    @Test
    void aViewShowsOnlyYourOwnCardsAndYourOwnLegalMoves() {
        GinState state = GAME.createInitialState(SEATS, Map.of(), ctx(8));
        GinView p1 = GAME.project(state, Optional.of(P1));
        GinView p0 = GAME.project(state, Optional.of(P0));
        assertEquals(state.handOf(P1), p1.myHand());
        for (Card card : state.handOf(P0)) assertFalse(p1.myHand().contains(card));
        assertEquals(List.of(DrawSource.STOCK, DrawSource.DISCARD), p1.drawSources());
        assertTrue(p0.drawSources().isEmpty(), "not your turn, no moves");
        assertEquals(10, p1.seats().get(0).cardCount());

        GinView spectator = GAME.project(state, Optional.empty());
        assertTrue(spectator.myHand().isEmpty());
        assertNull(spectator.myMelds());
    }

    @Test
    void knockAndGinDiscardsAreListedForTheDiscarder() {
        GinState state = discarding(cards("AC", "2C", "3C", "4D", "4H", "4S", "7S", "8S", "9S", "10S", "KD"),
                cards("5D", "6D", "7D", "10C", "JC", "QD", "KC", "9H", "3H", "4C"));
        GinView view = GAME.project(state, Optional.of(P0));
        assertEquals(11, view.discards().size());
        assertEquals(List.of(c("KD")), view.ginDiscards());
        assertTrue(view.knockDiscards().contains(c("KD")));
    }

    @Test
    void seatsAreFixedOnceDealt() {
        GinState state = GAME.createInitialState(SEATS, Map.of(), ctx(1));
        assertEquals(ErrorCode.SEAT_UNAVAILABLE,
                assertInstanceOf(Validation.Reject.class, GAME.canJoin(state, new Seat(2, new PlayerId("x"), "x"))).code());
    }

    @Test
    void parsesBothIntentsAndRejectsJunk() throws Exception {
        assertEquals(new Draw(DrawSource.DISCARD), GAME.parseIntent(MAPPER.readTree("{\"type\":\"draw\",\"source\":\"discard\"}")));
        assertEquals(new Discard(c("QS"), true), GAME.parseIntent(MAPPER.readTree(
                "{\"type\":\"discard\",\"card\":{\"rank\":\"QUEEN\",\"suit\":\"SPADES\"},\"knock\":true}")));
        assertEquals(new Discard(c("2H"), false), GAME.parseIntent(MAPPER.readTree(
                "{\"type\":\"discard\",\"card\":{\"rank\":\"TWO\",\"suit\":\"HEARTS\"}}")));
        for (String junk : List.of("{\"type\":\"draw\"}", "{\"type\":\"draw\",\"source\":\"deck\"}", "{\"type\":\"play\"}",
                "{\"type\":\"discard\"}", "{\"type\":\"discard\",\"card\":{\"rank\":\"TWO\",\"suit\":\"HEARTS\"},\"knock\":\"yes\"}")) {
            assertThrows(IllegalArgumentException.class, () -> GAME.parseIntent(MAPPER.readTree(junk)), junk);
        }
    }

    // ---- property: whole games driven by the timeout path ----

    /**
     * Plays 200 seeded games using nothing but {@code onTimeout}: every timeout move must
     * validate (or a room freezes alive), no card may appear or vanish, and games must end.
     */
    @Test
    void wholeGamesPlayedOnTimeoutsAlwaysReachGameOver() {
        for (long seed = 0; seed < 200; seed++) {
            EngineContext ctx = ctx(seed);
            GinState state = GAME.createInitialState(SEATS, Map.of(), ctx);
            for (int moves = 0; state.phase() != GinPhase.GAME_OVER; moves++) {
                assertTrue(moves < 50_000, "seed " + seed + " never ended");
                PlayerId actor = GAME.turn(state).orElseThrow().actor();
                state = step(state, GAME.onTimeout(state, actor), actor, ctx);

                Set<Card> all = new HashSet<>(state.stock());
                all.addAll(state.discard());
                state.hands().values().forEach(all::addAll);
                int count = state.stock().size() + state.discard().size()
                        + state.hands().values().stream().mapToInt(List::size).sum();
                assertEquals(52, count, "seed " + seed + ": card count");
                assertEquals(52, all.size(), "seed " + seed + ": a card is in two places");
            }
            GinState over = state;
            assertTrue(over.scores().values().stream().anyMatch(score -> score >= 100));
            assertEquals(over.scores().values().stream().mapToInt(Integer::intValue).sum(),
                    over.history().stream().flatMap(List::stream).mapToInt(Integer::intValue).sum());
        }
    }
}
