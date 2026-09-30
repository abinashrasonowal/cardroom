package com.whitejack.games.poker;

import com.whitejack.contract.Card;
import com.whitejack.contract.PlayerId;
import java.util.List;

/**
 * How the previous hand ended, kept so the table can show it after the next deal. Public: at a
 * showdown every live hand is turned face up, and a hand won uncontested shows nothing.
 *
 * @param board the community cards that were face up when the hand ended
 * @param reveals every hand shown down, in seat order; empty when everyone else folded
 * @param pots the main pot then each side pot, with who took it
 */
public record HandResult(int hand, List<Card> board, List<Reveal> reveals, List<Pot> pots) {

    public HandResult {
        board = List.copyOf(board);
        reveals = List.copyOf(reveals);
        pots = List.copyOf(pots);
    }

    /** {@code handName} is null when the pot was not contested. */
    public record Pot(int amount, List<PlayerId> winners, String handName) {
        public Pot {
            winners = List.copyOf(winners);
        }
    }

    /** {@code best} is the five cards that made {@code handName}. */
    public record Reveal(PlayerId player, List<Card> cards, String handName, List<Card> best) {
        public Reveal {
            cards = List.copyOf(cards);
            best = List.copyOf(best);
        }
    }
}
