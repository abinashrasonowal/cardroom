package com.whitejack.games.ginrummy;

public enum GinPhase {
    /** The player on turn must take a card from the stock or the discard pile. */
    DRAW,
    /** The player on turn holds 11 cards and must discard one, optionally knocking. */
    DISCARD,
    /**
     * The hand is scored and the next deal is on its way. Never a resting state: the discard
     * that ends a hand also emits the next {@code Dealt}.
     */
    SCORING,
    GAME_OVER
}
