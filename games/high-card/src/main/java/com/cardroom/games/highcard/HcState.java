package com.cardroom.games.highcard;

import com.cardroom.contract.Card;
import com.cardroom.contract.Deck;
import com.cardroom.contract.PlayerId;
import com.cardroom.contract.Seat;
import java.util.List;
import java.util.Map;

/**
 * Deeply immutable, as engine invariant 2 requires: {@code Deck} is a record over a copied
 * list and both collections here are re-copied on construction, so no caller retains a handle
 * that could mutate a state another thread is projecting.
 *
 * <p>Whose turn it is is <em>derived</em> — players draw in seat order, so the next actor is
 * {@code seats.get(drawn.size())}. Storing an index as well would be a second source of truth
 * that can disagree with the first.
 */
public record HcState(Deck deck, List<Seat> seats, Map<PlayerId, Card> drawn) {

    public HcState {
        seats = List.copyOf(seats);
        drawn = Map.copyOf(drawn);
    }

    boolean allDrawn() {
        return drawn.size() >= seats.size();
    }
}
