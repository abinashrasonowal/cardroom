package com.cardroom.games.hearts;

public enum HeartsPhase {
    /** Everyone picks three cards at once; the exchange happens when the fourth pass lands. */
    PASSING,
    PLAYING,
    /**
     * The hand is scored and the next deal is on its way. Never a resting state: the same
     * {@code reduce} that finishes trick 13 also emits the next {@code Dealt}.
     */
    SCORING,
    GAME_OVER
}
