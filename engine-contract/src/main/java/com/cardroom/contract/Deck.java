package com.cardroom.contract;

import java.util.ArrayList;
import java.util.List;

/**
 * An immutable pile of cards. Dealing returns the cards <em>and</em> the remaining deck rather
 * than mutating in place: a {@code record BjState(Deck deck)} holding a mutable deck would be
 * immutable only at the façade, and engine invariant 2 — every {@code S} deeply immutable, so
 * {@code project} and {@code replay} are safe off the room thread — would be false.
 *
 * <p>Index 0 is the top of the deck.
 */
public record Deck(List<Card> cards) {

    public Deck {
        cards = List.copyOf(cards);
    }

    public static Deck standard52() {
        List<Card> cards = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(rank, suit));
            }
        }
        return new Deck(cards);
    }

    public Deck shuffled(RandomSource rng) {
        return new Deck(Shuffler.shuffle(cards, rng));
    }

    /** Takes {@code n} cards off the top. Both halves come back; nothing is mutated. */
    public Deal deal(int n) {
        if (n < 0) throw new IllegalArgumentException("negative deal: " + n);
        if (n > cards.size()) {
            throw new IllegalArgumentException("deal " + n + " from a deck of " + cards.size());
        }
        return new Deal(cards.subList(0, n), new Deck(cards.subList(n, cards.size())));
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /** What was dealt, and what is left. */
    public record Deal(List<Card> cards, Deck rest) {
        public Deal {
            cards = List.copyOf(cards);
        }

        /** Convenience for the one-card case. */
        public Card card() {
            if (cards.size() != 1) {
                throw new IllegalStateException("expected exactly one card, got " + cards.size());
            }
            return cards.get(0);
        }
    }
}
