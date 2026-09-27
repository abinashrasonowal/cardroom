package com.cardroom.games.hearts;

import com.cardroom.contract.Card;
import com.cardroom.contract.GameEvent;
import com.cardroom.contract.PlayerId;
import java.util.List;
import java.util.Map;

/**
 * What happened. {@link Dealt} carries every card it dealt, which is what keeps {@code apply}
 * a pure fold: randomness is spent once, in {@code reduce}, and replay just reads it back.
 * These carry hidden cards by construction, so they never leave the JVM (§7).
 */
public sealed interface HeartsEvent extends GameEvent {

    record Dealt(int hand, Map<PlayerId, List<Card>> hands) implements HeartsEvent {
        public Dealt {
            hands = HeartsState.copyHands(hands);
        }
    }

    record Passed(PlayerId player, List<Card> cards) implements HeartsEvent {
        public Passed {
            cards = List.copyOf(cards);
        }
    }

    record Played(PlayerId player, Card card) implements HeartsEvent {}
}
