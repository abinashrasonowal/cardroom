package com.cardroom.contract;

/**
 * Closed enum sent on the wire as {@code error}. Prose belongs in {@code detail}; a client
 * switches on this. Grow it deliberately — every value is public API.
 */
public enum ErrorCode {
    NOT_YOUR_TURN,
    WRONG_PHASE,
    ILLEGAL_MOVE,
    UNKNOWN_INTENT,
    MALFORMED_INTENT,
    SEAT_UNAVAILABLE,
    NOT_SEATED
}
