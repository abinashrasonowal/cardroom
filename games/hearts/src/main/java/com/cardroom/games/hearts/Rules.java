package com.cardroom.games.hearts;

import com.cardroom.contract.Card;
import com.cardroom.contract.PlayerId;
import com.cardroom.contract.Rank;
import com.cardroom.contract.Suit;
import java.util.Comparator;
import java.util.List;

/** The rules of play, as pure functions of the state. The client never reimplements these. */
final class Rules {

    static final Card TWO_OF_CLUBS = new Card(Rank.TWO, Suit.CLUBS);
    static final Card QUEEN_OF_SPADES = new Card(Rank.QUEEN, Suit.SPADES);

    /** Hands are shown grouped by suit, then low to high. */
    static final Comparator<Card> BY_SUIT =
            Comparator.comparing(Card::suit).thenComparingInt(card -> card.rank().value());

    private Rules() {}

    static int points(Card card) {
        if (card.suit() == Suit.HEARTS) return 1;
        return card.equals(QUEEN_OF_SPADES) ? 13 : 0;
    }

    /** Highest card of the suit that was led. Off-suit cards cannot win, however high. */
    static PlayerId trickWinner(List<PlayedCard> trick) {
        Suit led = trick.get(0).card().suit();
        return trick.stream()
                .filter(play -> play.card().suit() == led)
                .max(Comparator.comparingInt(play -> play.card().rank().value()))
                .orElseThrow()
                .player();
    }

    /**
     * Every card {@code player} may play now, assuming it is their turn. Each restriction
     * gives way when the hand holds nothing else, so the list is never empty for a player who
     * still has cards — which is what lets {@code onTimeout} always find a legal play.
     */
    static List<Card> legalPlays(HeartsState state, PlayerId player) {
        List<Card> hand = state.handOf(player);
        boolean firstTrick = state.tricksPlayed() == 0;

        if (state.trick().isEmpty()) {
            if (firstTrick && hand.contains(TWO_OF_CLUBS)) return List.of(TWO_OF_CLUBS);
            if (!state.heartsBroken()) {
                List<Card> nonHearts = hand.stream().filter(card -> card.suit() != Suit.HEARTS).toList();
                if (!nonHearts.isEmpty()) return nonHearts;
            }
            return hand;
        }

        Suit led = state.trick().get(0).card().suit();
        List<Card> following = hand.stream().filter(card -> card.suit() == led).toList();
        if (!following.isEmpty()) return following;
        if (firstTrick) {
            List<Card> clean = hand.stream().filter(card -> points(card) == 0).toList();
            if (!clean.isEmpty()) return clean;
        }
        return hand;
    }

    /** Why {@code card} is not in {@link #legalPlays}, for the rejection's {@code detail}. */
    static String whyIllegal(HeartsState state, Card card) {
        // Only reached for the player on the clock, so their hand is the one that matters.
        if (state.trick().isEmpty()) {
            if (state.tricksPlayed() == 0) return "the 2♣ leads the first trick";
            return "hearts have not been broken yet";
        }
        Suit led = state.trick().get(0).card().suit();
        boolean canFollow = state.handOf(HeartsGame.onClock(state))
                .stream().anyMatch(held -> held.suit() == led);
        if (canFollow) return "you must follow " + led.symbol();
        return "no points on the first trick";
    }
}
