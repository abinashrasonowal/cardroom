package com.whitejack.games.poker;

import com.whitejack.contract.Card;
import com.whitejack.contract.GameEvent;
import com.whitejack.contract.PlayerId;
import java.util.List;
import java.util.Map;

/**
 * What happened. {@link Dealt} carries the hole cards <em>and</em> all five community cards,
 * so randomness is spent once, in {@code reduce}, and {@code apply} stays a pure fold; the board
 * is revealed street by street from what was already dealt. These carry hidden cards by
 * construction, so they never leave the JVM (§7).
 */
public sealed interface PokerEvent extends GameEvent {

    record Dealt(int hand, int dealer, int smallBlind, int bigBlind, Map<PlayerId, List<Card>> holes, List<Card> board)
            implements PokerEvent {
        public Dealt {
            holes = PokerState.copyHoles(holes);
            board = List.copyOf(board);
        }
    }

    /** {@code to} is the raise target for {@link Action#RAISE} and 0 otherwise. */
    record Acted(PlayerId player, Action action, int to) implements PokerEvent {}

    enum Action {
        FOLD,
        CHECK,
        CALL,
        RAISE
    }
}
