package com.whitejack.games.poker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.whitejack.contract.Card;
import com.whitejack.contract.EngineContext;
import com.whitejack.contract.ErrorCode;
import com.whitejack.contract.PlayerId;
import com.whitejack.contract.RandomSource;
import com.whitejack.contract.Rank;
import com.whitejack.contract.Seat;
import com.whitejack.contract.Suit;
import com.whitejack.contract.Validation;
import com.whitejack.games.poker.PokerEvent.Dealt;
import com.whitejack.games.poker.PokerIntent.Call;
import com.whitejack.games.poker.PokerIntent.Check;
import com.whitejack.games.poker.PokerIntent.Fold;
import com.whitejack.games.poker.PokerIntent.Raise;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PokerGameTest {

    private static final PokerGame GAME = new PokerGame();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final PlayerId P0 = new PlayerId("p0");
    private static final PlayerId P1 = new PlayerId("p1");
    private static final PlayerId P2 = new PlayerId("p2");
    private static final PlayerId P3 = new PlayerId("p3");
    private static final List<Seat> FOUR = List.of(new Seat(0, P0, "abi"), new Seat(1, P1, "sam"),
            new Seat(2, P2, "kim"), new Seat(3, P3, "lee"));
    private static final List<Seat> TWO = FOUR.subList(0, 2);

    /** Stands in for HmacRandom, which lives in engine-core and must never be on this classpath. */
    private static EngineContext ctx(long seed, List<Seat> seats) {
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
                return seats;
            }
        };
    }

    /** The engine's loop from §9, minus the log: validate, reduce, fold. */
    private static PokerState step(PokerState state, PokerIntent intent, PlayerId actor, EngineContext ctx) {
        Validation verdict = GAME.validate(state, intent, actor);
        assertTrue(verdict.isOk(), () -> actor + " " + intent + " rejected: " + verdict);
        PokerState next = state;
        for (PokerEvent event : GAME.reduce(state, intent, actor, ctx)) next = GAME.apply(next, event);
        return next;
    }

    private static ErrorCode rejection(PokerState state, PokerIntent intent, PlayerId actor) {
        return assertInstanceOf(Validation.Reject.class, GAME.validate(state, intent, actor)).code();
    }

    /** "QS", "10H", "AC". */
    private static Card c(String code) {
        String rank = code.substring(0, code.length() - 1);
        Suit suit = switch (code.charAt(code.length() - 1)) {
            case 'S' -> Suit.SPADES;
            case 'H' -> Suit.HEARTS;
            case 'D' -> Suit.DIAMONDS;
            default -> Suit.CLUBS;
        };
        for (Rank r : Rank.values()) if (r.symbol().equals(rank)) return new Card(r, suit);
        throw new IllegalArgumentException(code);
    }

    private static List<Card> cards(String codes) {
        return Arrays.stream(codes.split(" ")).map(PokerGameTest::c).toList();
    }

    /** A hand with chosen cards and stacks, the way {@code createInitialState} would fold it. */
    private static PokerState rigged(List<Seat> seats, int dealer, List<Integer> stacks, List<String> holes, String board) {
        Map<PlayerId, Integer> stackMap = new LinkedHashMap<>();
        Map<PlayerId, List<Card>> holeMap = new LinkedHashMap<>();
        for (int i = 0; i < seats.size(); i++) {
            stackMap.put(seats.get(i).id(), stacks.get(i));
            if (stacks.get(i) > 0) holeMap.put(seats.get(i).id(), cards(holes.get(i)));
        }
        PokerState seated = new PokerState(seats, -1, PokerPhase.SETTLED, Street.PREFLOP, dealer, 0, 0, Map.of(),
                List.of(), stackMap, Map.of(), Map.of(), Set.of(), Set.of(), Map.of(), 0, 0, null, null);
        return GAME.apply(seated, new Dealt(0, dealer, 10, 20, holeMap, cards(board)));
    }

    private static int chipsOnTable(PokerState state) {
        return state.stacks().values().stream().mapToInt(Integer::intValue).sum() + state.pot();
    }

    // ---- hand values ----

    @Test
    void handCategoriesRankInOrder() {
        List<String> ascending = List.of(
                "AS KD 9C 7H 3S",   // high card
                "2S 2D 9C 7H 3S",   // pair
                "2S 2D 9C 9H 3S",   // two pair
                "2S 2D 2C 9H 3S",   // trips
                "AS 2D 3C 4H 5S",   // wheel
                "6S 7D 8C 9H 10S",  // straight
                "2H 7H 9H JH KH",   // flush
                "2S 2D 2C 9H 9S",   // full house
                "2S 2D 2C 2H 9S",   // quads
                "5H 6H 7H 8H 9H");  // straight flush
        for (int i = 1; i < ascending.size(); i++) {
            HandValue lower = HandValue.best(cards(ascending.get(i - 1)));
            HandValue higher = HandValue.best(cards(ascending.get(i)));
            assertTrue(higher.compareTo(lower) > 0, ascending.get(i) + " should beat " + ascending.get(i - 1));
        }
    }

    @Test
    void theWheelIsFiveHighAndTheBestOfSevenIsFound() {
        HandValue wheel = HandValue.best(cards("AS 2D 3C 4H 5S KD KC"));
        assertEquals(HandValue.Category.STRAIGHT, wheel.category());
        assertEquals(Rank.FIVE, wheel.cards().get(0).rank(), "the ace plays low");
        assertTrue(HandValue.best(cards("2S 3D 4C 5H 6S KD KC")).compareTo(wheel) > 0, "six-high beats the wheel");

        assertEquals("Royal flush", HandValue.best(cards("AH KH QH JH 10H 2C 3D")).name());
        assertEquals("Full house", HandValue.best(cards("QS QD QC 9H 9S 9D 2C")).name(), "two trips make a full house");
    }

    @Test
    void kickersBreakTiesAndIdenticalHandsTie() {
        HandValue aceKicker = HandValue.best(cards("KS KD AC 7H 3S"));
        HandValue queenKicker = HandValue.best(cards("KC KH QC 7D 3D"));
        assertTrue(aceKicker.compareTo(queenKicker) > 0);
        assertEquals(0, HandValue.best(cards("KS KD AC 7H 3S")).compareTo(HandValue.best(cards("KC KH AD 7D 3D"))));
        assertTrue(HandValue.best(cards("9S 9D 4C 4H AS")).compareTo(HandValue.best(cards("9C 9H 8C 2D KD"))) > 0,
                "two pair beats one pair however high the kicker");
    }

    // ---- the deal and the blinds ----

    @Test
    void theFirstHandPostsBlindsAndHidesOtherPlayersCards() {
        PokerState state = GAME.createInitialState(FOUR, Map.of(), ctx(1, FOUR));

        assertEquals(PokerPhase.BETTING, state.phase());
        assertEquals(0, state.dealer(), "the button starts at seat 0");
        assertEquals(990, state.stackOf(P1), "small blind to the button's left");
        assertEquals(980, state.stackOf(P2), "big blind next");
        assertEquals(P3, state.toAct(), "first to act sits left of the big blind");
        assertEquals(4000, chipsOnTable(state));

        PokerView view = GAME.project(state, Optional.of(P3));
        assertEquals(2, view.myCards().size());
        assertTrue(view.board().isEmpty(), "no community cards before the flop");
        assertEquals(List.of("fold", "call", "raise"), view.legal());
        assertEquals(20, view.toCall());
        assertEquals(40, view.minRaiseTo());
        assertEquals(1000, view.maxRaiseTo());
        assertTrue(GAME.project(state, Optional.of(P0)).legal().isEmpty(), "not P0's move");
        assertTrue(GAME.project(state, Optional.empty()).myCards().isEmpty(), "spectators see no hole cards");

        Set<Card> dealt = new HashSet<>(state.board());
        state.holes().values().forEach(dealt::addAll);
        assertEquals(4 * 2 + 5, dealt.size(), "every card dealt is distinct");
    }

    @Test
    void headsUpTheButtonPostsTheSmallBlindAndActsFirstOnlyBeforeTheFlop() {
        EngineContext ctx = ctx(2, TWO);
        PokerState state = GAME.createInitialState(TWO, Map.of(), ctx);
        assertEquals(990, state.stackOf(P0), "the button is the small blind");
        assertEquals(980, state.stackOf(P1));
        assertEquals(P0, state.toAct());

        state = step(state, new Call(), P0, ctx);
        assertEquals(P1, state.toAct(), "the big blind has the option");
        state = step(state, new Check(), P1, ctx);
        assertEquals(Street.FLOP, state.street());
        assertEquals(P1, state.toAct(), "after the flop the big blind acts first");
        assertEquals(3, GAME.project(state, Optional.of(P0)).board().size());
    }

    // ---- betting ----

    @Test
    void foldingToTheBigBlindWinsTheBlindsAndDealsTheNextHand() {
        EngineContext ctx = ctx(3, FOUR);
        PokerState state = GAME.createInitialState(FOUR, Map.of(), ctx);
        state = step(state, new Fold(), P3, ctx);
        state = step(state, new Fold(), P0, ctx);
        state = step(state, new Fold(), P1, ctx);

        assertEquals(1, state.hand(), "the next hand is already dealt");
        assertEquals(1, state.dealer(), "the button moved one seat");
        HandResult last = state.lastHand();
        assertEquals(List.of(P2), last.pots().get(0).winners());
        assertEquals(30, last.pots().get(0).amount());
        assertTrue(last.reveals().isEmpty(), "an uncontested pot shows no cards");
        assertNull(last.pots().get(0).handName());
        // P2 won 30 with 20 of it their own blind, then posts the small blind in the new hand.
        assertEquals(1010 - 10, state.stackOf(P2));
        assertEquals(4000, chipsOnTable(state));
    }

    @Test
    void raisesMustBeAtLeastTheLastRaiseUnlessAllIn() {
        EngineContext ctx = ctx(4, FOUR);
        PokerState state = GAME.createInitialState(FOUR, Map.of(), ctx);

        assertEquals(ErrorCode.ILLEGAL_MOVE, rejection(state, new Raise(30), P3), "a raise to 30 is less than a full raise");
        assertEquals(ErrorCode.ILLEGAL_MOVE, rejection(state, new Check(), P3), "20 to call");
        assertEquals(ErrorCode.ILLEGAL_MOVE, rejection(state, new Raise(1001), P3), "more than P3 has");
        assertEquals(ErrorCode.NOT_YOUR_TURN, rejection(state, new Call(), P0));

        state = step(state, new Raise(60), P3, ctx);
        assertEquals(60, state.currentBet());
        assertEquals(40, state.minRaise(), "the raise was 40");
        assertEquals(100, GAME.project(state, Optional.of(P0)).minRaiseTo());
        assertEquals(ErrorCode.ILLEGAL_MOVE, rejection(state, new Raise(90), P0));
        state = step(state, new Raise(1000), P0, ctx);
        assertEquals("All-in 1000", state.lastActions().get(P0));
        assertEquals(0, state.stackOf(P0));
    }

    @Test
    void checkingItDownRevealsTheBoardAndPaysTheBestHand() {
        // P1 holds aces; the board gives nobody better.
        PokerState state = rigged(TWO, 0, List.of(1000, 1000), List.of("7C 2D", "AS AH"), "KD 9S 4C 3H JD");
        EngineContext ctx = ctx(5, TWO);
        state = step(state, new Call(), P0, ctx);
        state = step(state, new Check(), P1, ctx);
        for (Street street : List.of(Street.FLOP, Street.TURN, Street.RIVER)) {
            assertEquals(street, state.street());
            assertEquals(street.boardCards(), GAME.project(state, Optional.of(P0)).board().size());
            state = step(state, new Check(), P1, ctx);
            state = step(state, new Check(), P0, ctx);
        }

        HandResult last = state.lastHand();
        assertEquals(List.of(P1), last.pots().get(0).winners());
        assertEquals("Pair", last.pots().get(0).handName());
        assertEquals(2, last.reveals().size(), "both hands are shown down");
        assertEquals(5, last.board().size());
        assertEquals(2000, chipsOnTable(state));
    }

    @Test
    void anAllInForLessBuildsASidePotTheShortStackCannotWin() {
        // P0 is short with the best hand; P1 beats P2 for the side pot.
        PokerState state = rigged(List.of(FOUR.get(0), FOUR.get(1), FOUR.get(2)), 2, List.of(100, 1000, 1000),
                List.of("AS AH", "KS KH", "QS QH"), "2C 5D 8H 9C JS");
        EngineContext ctx = ctx(6, FOUR);
        // Button P2, small blind P0, big blind P1; P2 acts first.
        assertEquals(P2, state.toAct());
        state = step(state, new Raise(300), P2, ctx);
        state = step(state, new Call(), P0, ctx);      // all-in for less
        state = step(state, new Call(), P1, ctx);
        // Flop onward: P1 and P2 check it down.
        while (state.hand() == 0) {
            state = step(state, new Check(), state.toAct(), ctx);
        }

        List<HandResult.Pot> pots = state.lastHand().pots();
        assertEquals(2, pots.size());
        assertEquals(300, pots.get(0).amount(), "main pot: 100 from each");
        assertEquals(List.of(P0), pots.get(0).winners());
        assertEquals(400, pots.get(1).amount(), "side pot: 200 more from each of the others");
        assertEquals(List.of(P1), pots.get(1).winners());
        assertEquals(2100, chipsOnTable(state));
    }

    @Test
    void uncalledChipsGoBackToTheBettor() {
        PokerState state = rigged(TWO, 0, List.of(1000, 200), List.of("2C 7D", "AS AH"), "KD 9S 4C 3H JD");
        EngineContext ctx = ctx(7, TWO);
        state = step(state, new Raise(1000), P0, ctx); // shove
        state = step(state, new Call(), P1, ctx);      // calls all-in for 200
        HandResult last = state.lastHand();
        assertEquals(400, last.pots().stream().mapToInt(HandResult.Pot::amount).sum(), "only 200 of P0's shove was called");
        assertEquals(List.of(P1), last.pots().get(0).winners());
        // P0 got 800 back; the next hand's blinds are already posted.
        assertEquals(1200, chipsOnTable(state));
    }

    @Test
    void aTieSplitsThePotWithTheOddChipLeftOfTheButton() {
        // P0 and P2 both make the ace-high straight; P1's trips lose.
        List<Seat> three = FOUR.subList(0, 3);
        PokerState state = rigged(three, 0, List.of(1000, 1000, 1000), List.of("AS 3C", "2C 2D", "AD 4C"),
                "10S JS QD KC 2H");
        EngineContext ctx = ctx(8, three);
        state = step(state, new Call(), P0, ctx);      // the button calls 20
        state = step(state, new Raise(45), P1, ctx);   // the small blind makes it 45
        state = step(state, new Call(), P2, ctx);
        state = step(state, new Call(), P0, ctx);
        while (state.hand() == 0) state = step(state, new Check(), state.toAct(), ctx);

        HandResult.Pot pot = state.lastHand().pots().get(0);
        assertEquals(135, pot.amount());
        assertEquals(List.of(P2, P0), pot.winners(), "clockwise from the button");
        assertEquals("Straight", pot.handName());
        // 135 splits 68 / 67, the odd chip to P2. Hand 1 has P2 on the small blind, P0 on the big.
        assertEquals(1000 - 45 + 68, state.stackOf(P2) + state.betOf(P2));
        assertEquals(1000 - 45 + 67, state.stackOf(P0) + state.betOf(P0));
        assertEquals(3000, chipsOnTable(state));
    }

    @Test
    void aBoardThatPlaysSplitsEvenly() {
        PokerState state = rigged(TWO, 0, List.of(1000, 1000), List.of("2C 3D", "4C 5D"), "10S JH QD KC AS");
        EngineContext ctx = ctx(9, TWO);
        state = step(state, new Raise(1000), P0, ctx);
        state = step(state, new Call(), P1, ctx);
        HandResult.Pot pot = state.lastHand().pots().get(0);
        assertEquals(List.of(P1, P0), pot.winners(), "clockwise from the button");
        assertEquals(2000, pot.amount());
        assertEquals(1000, state.stackOf(P0) + state.betOf(P0), "both back to 1000 before the next blinds");
        assertEquals(1000, state.stackOf(P1) + state.betOf(P1));
    }

    // ---- timeouts and whole games ----

    @Test
    void timeoutChecksWhenFreeAndFoldsOtherwise() {
        EngineContext ctx = ctx(10, TWO);
        PokerState state = GAME.createInitialState(TWO, Map.of(), ctx);
        assertInstanceOf(Fold.class, GAME.onTimeout(state, P0), "the small blind owes 10");
        state = step(state, new Call(), P0, ctx);
        assertInstanceOf(Check.class, GAME.onTimeout(state, P1), "the big blind can check");
    }

    /**
     * Random legal play to the end, several times over: chips are never made or lost, every
     * timeout move is legal, and the game always finishes with one player holding everything.
     */
    @Test
    void randomGamesConserveChipsAndAlwaysFinish() {
        for (long seed = 0; seed < 40; seed++) {
            List<Seat> seats = FOUR.subList(0, 2 + (int) (seed % 3));
            EngineContext ctx = ctx(seed, seats);
            Random choices = new Random(seed);
            PokerState state = GAME.createInitialState(seats, Map.of(), ctx);
            int total = seats.size() * PokerGame.STARTING_STACK;

            int moves = 0;
            while (!GAME.isComplete(state)) {
                assertTrue(++moves < 20_000, "seed " + seed + " never finished");
                PlayerId actor = GAME.turn(state).orElseThrow().actor();
                assertTrue(GAME.validate(state, GAME.onTimeout(state, actor), actor).isOk(), "timeout move must be legal");

                PokerView view = GAME.project(state, Optional.of(actor));
                String move = view.legal().get(choices.nextInt(view.legal().size()));
                PokerIntent intent = switch (move) {
                    case "fold" -> choices.nextInt(4) == 0 ? new Fold() : (view.toCall() == 0 ? new Check() : new Call());
                    case "check" -> new Check();
                    case "call" -> new Call();
                    default -> new Raise(choices.nextBoolean() ? view.maxRaiseTo()
                            : view.minRaiseTo() + choices.nextInt(view.maxRaiseTo() - view.minRaiseTo() + 1));
                };
                state = step(state, intent, actor, ctx);
                assertEquals(total, chipsOnTable(state), "seed " + seed + ": chips changed");
                state.stacks().values().forEach(stack -> assertTrue(stack >= 0));
            }

            assertEquals(PokerPhase.GAME_OVER, state.phase());
            assertFalse(GAME.turn(state).isPresent());
            PokerView end = GAME.project(state, Optional.of(P0));
            assertEquals(total, state.stackOf(end.winner()));
        }
    }

    @Test
    void parsesTheWireForms() throws Exception {
        assertInstanceOf(Fold.class, GAME.parseIntent(MAPPER.readTree("{\"type\":\"fold\"}")));
        assertInstanceOf(Check.class, GAME.parseIntent(MAPPER.readTree("{\"type\":\"check\"}")));
        assertInstanceOf(Call.class, GAME.parseIntent(MAPPER.readTree("{\"type\":\"call\"}")));
        assertEquals(new Raise(120), GAME.parseIntent(MAPPER.readTree("{\"type\":\"raise\",\"to\":120}")));
        assertThrows(IllegalArgumentException.class, () -> GAME.parseIntent(MAPPER.readTree("{\"type\":\"raise\"}")));
        assertThrows(IllegalArgumentException.class, () -> GAME.parseIntent(MAPPER.readTree("{\"type\":\"draw\"}")));
    }

    @Test
    void seatsAreFixedOnceDealt() {
        PokerState state = GAME.createInitialState(TWO, Map.of(), ctx(11, TWO));
        assertEquals(ErrorCode.SEAT_UNAVAILABLE,
                assertInstanceOf(Validation.Reject.class, GAME.canJoin(state, FOUR.get(2))).code());
        assertEquals(ErrorCode.NOT_SEATED, rejection(state, new Fold(), P3));
        List<Seat> nine = new ArrayList<>();
        for (int i = 0; i < 9; i++) nine.add(new Seat(i, new PlayerId("x" + i), "x" + i));
        assertThrows(IllegalStateException.class, () -> GAME.createInitialState(nine, Map.of(), ctx(12, nine)));
    }
}
