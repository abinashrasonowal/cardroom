package com.whitejack.games.poker;

/** A betting round, and how many community cards are face up during it. */
public enum Street {
    PREFLOP(0),
    FLOP(3),
    TURN(4),
    RIVER(5);

    private final int boardCards;

    Street(int boardCards) {
        this.boardCards = boardCards;
    }

    public int boardCards() {
        return boardCards;
    }

    Street next() {
        return values()[ordinal() + 1];
    }
}
