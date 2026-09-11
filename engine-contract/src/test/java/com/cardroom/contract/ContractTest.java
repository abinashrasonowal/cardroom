package com.cardroom.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ContractTest {

    /** Deterministic stand-in for HmacRandom (step 2). Same seed in, same stream out. */
    private static RandomSource lcg(long seed) {
        long[] s = {seed};
        return bound -> {
            s[0] = s[0] * 6364136223846793005L + 1442695040888963407L;
            return (int) Long.remainderUnsigned(s[0] >>> 33, bound);
        };
    }

    @Test
    void standard52HasFiftyTwoDistinctCards() {
        Deck deck = Deck.standard52();
        assertEquals(52, deck.size());
        assertEquals(52, new HashSet<>(deck.cards()).size());
        for (Suit suit : Suit.values()) {
            assertEquals(13, deck.cards().stream().filter(c -> c.suit() == suit).count());
        }
    }

    @Test
    void shuffleIsAPermutation() {
        Deck shuffled = Deck.standard52().shuffled(lcg(42));
        assertEquals(52, shuffled.size());
        assertEquals(new HashSet<>(Deck.standard52().cards()), new HashSet<>(shuffled.cards()));
        assertNotEquals(Deck.standard52().cards(), shuffled.cards(), "a shuffle that changes nothing is not a shuffle");
    }

    @Test
    void sameSeedDealsTheSameCards() {
        assertEquals(
                Deck.standard52().shuffled(lcg(42)).cards(),
                Deck.standard52().shuffled(lcg(42)).cards());
        assertNotEquals(
                Deck.standard52().shuffled(lcg(42)).cards(),
                Deck.standard52().shuffled(lcg(43)).cards());
    }

    @Test
    void everyPositionIsReachable() {
        // Fisher-Yates must be able to move the top card anywhere, not just down a bit.
        Card top = Deck.standard52().cards().get(0);
        Set<Integer> landings = new HashSet<>();
        for (long seed = 0; seed < 200; seed++) {
            landings.add(Deck.standard52().shuffled(lcg(seed)).cards().indexOf(top));
        }
        assertTrue(landings.size() > 40, "top card only reached " + landings.size() + " positions");
    }

    @Test
    void dealSplitsTheDeckWithoutMutatingIt() {
        Deck deck = Deck.standard52();
        Deck.Deal deal = deck.deal(5);

        assertEquals(5, deal.cards().size());
        assertEquals(47, deal.rest().size());
        assertEquals(52, deck.size(), "the original deck is untouched");

        List<Card> rejoined = new ArrayList<>(deal.cards());
        rejoined.addAll(deal.rest().cards());
        assertEquals(deck.cards(), rejoined, "deal must preserve order, not just membership");
    }

    @Test
    void dealingMoreThanTheDeckHoldsThrows() {
        assertThrows(IllegalArgumentException.class, () -> Deck.standard52().deal(53));
        assertThrows(IllegalArgumentException.class, () -> Deck.standard52().deal(-1));
        assertEquals(0, Deck.standard52().deal(0).cards().size());
        assertTrue(Deck.standard52().deal(52).rest().isEmpty());
    }

    @Test
    void deckDefendsItsOwnContents() {
        List<Card> source = new ArrayList<>(List.of(Card.of(Rank.ACE, Suit.SPADES)));
        Deck deck = new Deck(source);

        source.clear(); // a caller keeping the list it handed in must not empty the deck
        assertEquals(1, deck.size());
        assertThrows(UnsupportedOperationException.class, () -> deck.cards().clear());
    }

    @Test
    void aceIsHigh() {
        List<Card> sorted = Deck.standard52().cards().stream().sorted().collect(Collectors.toList());
        assertEquals(Rank.TWO, sorted.get(0).rank());
        assertEquals(Rank.ACE, sorted.get(51).rank());
    }

    @Test
    void roomCodesAreCaseInsensitive() {
        assertEquals(new RoomCode("K7M2QX"), new RoomCode("k7m2qx"));
        assertThrows(IllegalArgumentException.class, () -> new RoomCode(" "));
    }

    @Test
    void rejectionCarriesAClosedCode() {
        assertTrue(Validation.OK.isOk());
        Validation r = Validation.reject(ErrorCode.NOT_YOUR_TURN, "seat 2 is on the clock");
        assertFalse(r.isOk());
        assertEquals(ErrorCode.NOT_YOUR_TURN, ((Validation.Reject) r).code());
    }
}
