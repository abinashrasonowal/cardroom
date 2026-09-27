package com.cardroom.games.ginrummy;

import com.cardroom.contract.Card;
import com.cardroom.contract.PlayerId;
import java.util.List;

/**
 * How the last hand ended. Both hands are public once a hand is over, which is why this can
 * sit in every player's view.
 *
 * @param knocker who knocked; null for a dead hand
 * @param winner who scored; null for a dead hand
 */
public record HandResult(Outcome outcome, PlayerId knocker, PlayerId winner, int points, List<Revealed> hands) {

    public enum Outcome {
        KNOCK,
        GIN,
        UNDERCUT,
        /** The stock ran down to two cards with nobody knocking: no score, redeal. */
        DEAD
    }

    /** One player's hand as laid down: melds, the deadwood left over, and cards laid off. */
    public record Revealed(PlayerId player, List<List<Card>> melds, List<Card> deadwood, List<Card> laidOff,
            int deadwoodPoints) {
        public Revealed {
            melds = melds.stream().map(List::copyOf).toList();
            deadwood = List.copyOf(deadwood);
            laidOff = List.copyOf(laidOff);
        }
    }

    public HandResult {
        hands = List.copyOf(hands);
    }
}
