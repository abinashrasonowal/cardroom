package com.whitejack.games.hearts;

/** Left, right, across, hold — one per hand, repeating. {@code offset} is in seat positions. */
public enum PassDirection {
    LEFT(1),
    RIGHT(3),
    ACROSS(2),
    HOLD(0);

    private final int offset;

    PassDirection(int offset) {
        this.offset = offset;
    }

    public int offset() {
        return offset;
    }

    public static PassDirection forHand(int hand) {
        return values()[hand % values().length];
    }
}
