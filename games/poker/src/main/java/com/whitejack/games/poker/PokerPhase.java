package com.whitejack.games.poker;

public enum PokerPhase {
    BETTING,
    /**
     * The hand is paid out and the next deal is on its way. Never a resting state: the same
     * {@code reduce} that ends a hand also emits the next {@code Dealt}.
     */
    SETTLED,
    /** One player holds every chip. */
    GAME_OVER
}
