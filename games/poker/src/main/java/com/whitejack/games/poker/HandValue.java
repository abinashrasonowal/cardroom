package com.whitejack.games.poker;

import com.whitejack.contract.Card;
import com.whitejack.contract.Rank;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The best five-card poker hand out of up to seven, as a single comparable {@code score}:
 * the category in the high bits, then up to five tie-break ranks, four bits each, most
 * significant first. Higher always wins; equal scores split the pot.
 *
 * @param cards the five cards that make the hand, in tie-break order
 */
public record HandValue(int score, Category category, List<Card> cards) implements Comparable<HandValue> {

    public HandValue {
        cards = List.copyOf(cards);
    }

    public enum Category {
        HIGH_CARD("High card"),
        PAIR("Pair"),
        TWO_PAIR("Two pair"),
        THREE_OF_A_KIND("Three of a kind"),
        STRAIGHT("Straight"),
        FLUSH("Flush"),
        FULL_HOUSE("Full house"),
        FOUR_OF_A_KIND("Four of a kind"),
        STRAIGHT_FLUSH("Straight flush");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** "Full house", or "Royal flush" for the one straight flush that has its own name. */
    public String name() {
        if (category == Category.STRAIGHT_FLUSH && cards.get(0).rank() == Rank.ACE) return "Royal flush";
        return category.label();
    }

    @Override
    public int compareTo(HandValue other) {
        return Integer.compare(score, other.score);
    }

    /** Every five-card subset of {@code cards} (21 of them for seven), keeping the best. */
    public static HandValue best(List<Card> cards) {
        if (cards.size() < 5) throw new IllegalArgumentException("a poker hand needs five cards, got " + cards.size());
        HandValue best = null;
        int n = cards.size();
        for (int a = 0; a < n; a++)
            for (int b = a + 1; b < n; b++)
                for (int c = b + 1; c < n; c++)
                    for (int d = c + 1; d < n; d++)
                        for (int e = d + 1; e < n; e++) {
                            HandValue value = ofFive(List.of(cards.get(a), cards.get(b), cards.get(c), cards.get(d), cards.get(e)));
                            if (best == null || value.score > best.score) best = value;
                        }
        return best;
    }

    static HandValue ofFive(List<Card> five) {
        // Group by rank, biggest group first, then higher rank: that order is also the tie-break order.
        Map<Integer, List<Card>> byRank = new TreeMap<>(Comparator.reverseOrder());
        for (Card card : five) byRank.computeIfAbsent(card.rank().value(), r -> new ArrayList<>()).add(card);
        List<List<Card>> groups = new ArrayList<>(byRank.values());
        groups.sort(Comparator.comparingInt((List<Card> g) -> g.size()).reversed()
                .thenComparing(g -> g.get(0).rank().value(), Comparator.reverseOrder()));
        List<Card> ordered = groups.stream().flatMap(List::stream).toList();

        boolean flush = five.stream().map(Card::suit).distinct().count() == 1;
        int straightHigh = straightHigh(byRank.keySet().stream().toList());

        if (straightHigh > 0) {
            List<Card> run = straightOrder(ordered, straightHigh);
            Category category = flush ? Category.STRAIGHT_FLUSH : Category.STRAIGHT;
            return new HandValue(score(category, List.of(straightHigh)), category, run);
        }
        if (flush) return of(Category.FLUSH, ordered);
        int top = groups.get(0).size();
        int second = groups.size() > 1 ? groups.get(1).size() : 0;
        if (top == 4) return of(Category.FOUR_OF_A_KIND, ordered);
        if (top == 3 && second == 2) return of(Category.FULL_HOUSE, ordered);
        if (top == 3) return of(Category.THREE_OF_A_KIND, ordered);
        if (top == 2 && second == 2) return of(Category.TWO_PAIR, ordered);
        if (top == 2) return of(Category.PAIR, ordered);
        return of(Category.HIGH_CARD, ordered);
    }

    private static HandValue of(Category category, List<Card> ordered) {
        // One tie-break rank per group: the quad, trips or pair, then each kicker.
        List<Integer> ranks = new ArrayList<>();
        for (Card card : ordered) {
            if (ranks.isEmpty() || ranks.get(ranks.size() - 1) != card.rank().value()) ranks.add(card.rank().value());
        }
        return new HandValue(score(category, ranks), category, ordered);
    }

    /** Five distinct ranks in a row, high card of the run; the wheel A-2-3-4-5 is five-high. 0 if none. */
    private static int straightHigh(List<Integer> distinctDescending) {
        if (distinctDescending.size() != 5) return 0;
        int high = distinctDescending.get(0);
        int low = distinctDescending.get(4);
        if (high - low == 4) return high;
        if (high == 14 && distinctDescending.get(1) == 5) return 5;
        return 0;
    }

    /** The run from its high card down, with a wheel's ace moved to the bottom. */
    private static List<Card> straightOrder(List<Card> ordered, int high) {
        if (high != 5) return ordered;
        List<Card> wheel = new ArrayList<>(ordered.subList(1, 5));
        wheel.add(ordered.get(0));
        return wheel;
    }

    private static int score(Category category, List<Integer> ranks) {
        int score = category.ordinal();
        for (int i = 0; i < 5; i++) score = (score << 4) | (i < ranks.size() ? ranks.get(i) : 0);
        return score;
    }
}
