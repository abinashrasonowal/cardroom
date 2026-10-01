package com.whitejack.bots.brain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.whitejack.contract.Card;
import com.whitejack.contract.Rank;
import com.whitejack.contract.Suit;
import java.util.List;
import org.junit.jupiter.api.Test;

class GinMeldsTest {

    private static Card c(Rank rank, Suit suit) {
        return Card.of(rank, suit);
    }

    @Test
    void ginHandHasNoDeadwood() {
        List<Card> hand = List.of(
                c(Rank.ACE, Suit.CLUBS), c(Rank.TWO, Suit.CLUBS), c(Rank.THREE, Suit.CLUBS),
                c(Rank.SEVEN, Suit.HEARTS), c(Rank.SEVEN, Suit.DIAMONDS), c(Rank.SEVEN, Suit.SPADES),
                c(Rank.TEN, Suit.SPADES), c(Rank.JACK, Suit.SPADES), c(Rank.QUEEN, Suit.SPADES), c(Rank.KING, Suit.SPADES));
        assertEquals(0, GinMelds.minDeadwood(hand));
        assertEquals(List.of(), GinMelds.deadwood(hand));
    }

    @Test
    void acesAreLowSoQueenKingAceIsNoRun() {
        List<Card> hand = List.of(c(Rank.QUEEN, Suit.HEARTS), c(Rank.KING, Suit.HEARTS), c(Rank.ACE, Suit.HEARTS));
        assertEquals(21, GinMelds.minDeadwood(hand));
    }

    @Test
    void picksTheBetterOfOverlappingSetAndRun() {
        // 7♥ fits both 5♥6♥7♥ and 7♥7♣7♦; using it in the set leaves 5+6 = 11, in the run 7+7 = 14.
        List<Card> hand = List.of(c(Rank.FIVE, Suit.HEARTS), c(Rank.SIX, Suit.HEARTS), c(Rank.SEVEN, Suit.HEARTS),
                c(Rank.SEVEN, Suit.CLUBS), c(Rank.SEVEN, Suit.DIAMONDS));
        assertEquals(11, GinMelds.minDeadwood(hand));
        assertEquals(List.of(c(Rank.FIVE, Suit.HEARTS), c(Rank.SIX, Suit.HEARTS)), GinMelds.deadwood(hand));
    }

    @Test
    void afterBestDiscardNeverThrowsTheKeptCard() {
        List<Card> hand = List.of(c(Rank.KING, Suit.CLUBS), c(Rank.TWO, Suit.CLUBS));
        assertEquals(2, GinMelds.afterBestDiscard(hand, null));
        assertEquals(10, GinMelds.afterBestDiscard(hand, c(Rank.KING, Suit.CLUBS)));
    }
}
