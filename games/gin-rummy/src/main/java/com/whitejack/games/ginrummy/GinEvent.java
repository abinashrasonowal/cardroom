package com.whitejack.games.ginrummy;

import com.whitejack.contract.Card;
import com.whitejack.contract.GameEvent;
import com.whitejack.contract.PlayerId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What happened. {@link Dealt} carries the whole stock in order, so {@code apply} never needs
 * randomness and replay reads back exactly the cards that were drawn.
 */
public sealed interface GinEvent extends GameEvent {

    record Dealt(int hand, PlayerId dealer, Map<PlayerId, List<Card>> hands, Card upcard, List<Card> stock)
            implements GinEvent {
        public Dealt {
            Map<PlayerId, List<Card>> copy = new LinkedHashMap<>();
            hands.forEach((player, cards) -> copy.put(player, List.copyOf(cards)));
            hands = Map.copyOf(copy);
            stock = List.copyOf(stock);
        }
    }

    record Drew(PlayerId player, DrawSource source, Card card) implements GinEvent {}

    record Discarded(PlayerId player, Card card, boolean knock) implements GinEvent {}
}
