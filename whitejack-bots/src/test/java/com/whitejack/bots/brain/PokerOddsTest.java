package com.whitejack.bots.brain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.whitejack.contract.Card;
import com.whitejack.contract.Rank;
import com.whitejack.contract.Suit;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class PokerOddsTest {

    /** "AS KH 10D" → cards. */
    private static List<Card> cards(String spec) {
        return Arrays.stream(spec.split(" ")).map(PokerOddsTest::card).toList();
    }

    private static Card card(String s) {
        String rank = s.substring(0, s.length() - 1);
        Rank r = Arrays.stream(Rank.values()).filter(x -> x.symbol().equals(rank)).findFirst().orElseThrow();
        Suit suit = switch (s.charAt(s.length() - 1)) {
            case 'C' -> Suit.CLUBS;
            case 'D' -> Suit.DIAMONDS;
            case 'H' -> Suit.HEARTS;
            default -> Suit.SPADES;
        };
        return Card.of(r, suit);
    }

    private static int category(String spec) {
        return PokerOdds.category(PokerOdds.score(cards(spec)));
    }

    @Test
    void categoriesRankInPokerOrder() {
        List<String> ascending = List.of(
                "AS KD 9C 7H 4D 3C 2S",   // high card
                "AS AD 9C 7H 4D 3C 2S",   // pair
                "AS AD 9C 9H 4D 3C 2S",   // two pair
                "AS AD AC 9H 4D 3C 2S",   // trips
                "AS 2D 3C 4H 5D 9C JS",   // wheel straight
                "AS KS 9S 7S 4S 3C 2D",   // flush
                "AS AD AC 9H 9D 3C 2S",   // full house
                "AS AD AC AH 9D 3C 2S",   // quads
                "9S 10S JS QS KS 3C 2D"); // straight flush
        for (int i = 0; i < ascending.size(); i++) {
            assertEquals(i, category(ascending.get(i)), ascending.get(i));
            if (i > 0) {
                assertTrue(PokerOdds.score(cards(ascending.get(i))) > PokerOdds.score(cards(ascending.get(i - 1))),
                        ascending.get(i));
            }
        }
    }

    @Test
    void tieBreaksUseKickersAndTheWheelIsTheLowestStraight() {
        assertTrue(PokerOdds.score(cards("AS AD KC 7H 4D")) > PokerOdds.score(cards("AS AD QC 7H 4D")));
        assertTrue(PokerOdds.score(cards("2S 3D 4C 5H 6D")) > PokerOdds.score(cards("AS 2D 3C 4H 5D")));
        assertEquals(PokerOdds.score(cards("AS AD KC 7H 4D 3C 2S")), PokerOdds.score(cards("AH AC KD 7S 4C 3D 2H")));
        assertEquals(6, category("KS KD KC QH QD QC 2S"), "two sets of trips make a full house");
    }

    @Test
    void describesTheMadeHand() {
        assertEquals("pair of jacks", PokerOdds.describe(cards("JS JD 9C 7H 4D")));
        assertEquals("two pair, aces and sixes", PokerOdds.describe(cards("AS AD 6C 6H 4D")));
        assertEquals("full house, kings full of queens", PokerOdds.describe(cards("KS KD KC QH QD")));
        assertEquals("straight, five high", PokerOdds.describe(cards("AS 2D 3C 4H 5D")));
    }

    @Test
    void pocketAcesWinAboutEightyFivePercentHeadsUp() {
        double equity = PokerOdds.equity(cards("AS AD"), List.of(), 1, 20_000, new Random(7));
        assertEquals(0.85, equity, 0.02);
        double threeWay = PokerOdds.equity(cards("AS AD"), List.of(), 2, 20_000, new Random(7));
        assertEquals(0.73, threeWay, 0.03);
    }

    @Test
    void aMadeFlushOnTheRiverIsNearlyCertain() {
        double equity = PokerOdds.equity(cards("AS KS"), cards("2S 7S 9S JD 3C"), 1, 5_000, new Random(1));
        assertTrue(equity > 0.97, String.valueOf(equity));
    }
}
