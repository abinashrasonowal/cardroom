package com.cardroom.contract;

import java.util.Comparator;
import java.util.Objects;

/** Natural order is ace-high by rank, then suit, so that sorting a hand is not each game's job. */
public record Card(Rank rank, Suit suit) implements Comparable<Card> {

    private static final Comparator<Card> ORDER =
            Comparator.comparingInt((Card c) -> c.rank.value()).thenComparing(Card::suit);

    public Card {
        Objects.requireNonNull(rank, "rank");
        Objects.requireNonNull(suit, "suit");
    }

    public static Card of(Rank rank, Suit suit) {
        return new Card(rank, suit);
    }

    @Override
    public int compareTo(Card other) {
        return ORDER.compare(this, other);
    }

    @Override
    public String toString() {
        return rank.symbol() + suit.symbol();
    }
}
