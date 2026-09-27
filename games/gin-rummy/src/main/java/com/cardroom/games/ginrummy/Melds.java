package com.cardroom.games.ginrummy;

import com.cardroom.contract.Card;
import com.cardroom.contract.Rank;
import com.cardroom.contract.Suit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Gin rummy arithmetic: card values, melds, and the arrangement of a hand with the least
 * deadwood. Aces are low — A-2-3 is a run, Q-K-A is not.
 */
public final class Melds {

    /** How a hand splits: melds, and the leftover deadwood with its point total. */
    public record Arrangement(List<List<Card>> melds, List<Card> deadwood, int deadwoodPoints) {
        public Arrangement {
            melds = melds.stream().map(List::copyOf).toList();
            deadwood = List.copyOf(deadwood);
        }
    }

    /** Hands are shown by suit, then A low to K high. */
    static final Comparator<Card> BY_SUIT = Comparator.comparing(Card::suit).thenComparingInt(Melds::rank);

    private Melds() {}

    /** Ace 1, then face value, J 11, Q 12, K 13. */
    static int rank(Card card) {
        return card.rank() == Rank.ACE ? 1 : card.rank().value();
    }

    /** Deadwood value: ace 1, pips at face value, court cards 10. */
    static int points(Card card) {
        return Math.min(rank(card), 10);
    }

    static int points(List<Card> cards) {
        return cards.stream().mapToInt(Melds::points).sum();
    }

    /** The arrangement with the least deadwood, found by exhaustive search — 11 cards is small. */
    static Arrangement best(List<Card> hand) {
        List<Card> cards = hand.stream().sorted(BY_SUIT).toList();
        List<Long> melds = candidateMelds(cards);
        long[] bestMask = {0};
        int[] bestCovered = {0};
        search(cards, melds, 0, 0L, 0, bestMask, bestCovered);

        List<List<Card>> chosen = new ArrayList<>();
        long used = 0;
        for (long meld : chosenMelds(cards, melds, bestMask[0])) {
            chosen.add(cardsOf(cards, meld));
            used |= meld;
        }
        List<Card> deadwood = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            if ((used & (1L << i)) == 0) deadwood.add(cards.get(i));
        }
        return new Arrangement(chosen, deadwood, points(deadwood));
    }

    /**
     * Branch and bound over candidate melds; {@code bestMask} records which melds (by index)
     * cover the most points. Melds are bitmasks over the sorted hand.
     */
    private static void search(List<Card> cards, List<Long> melds, int from, long used, int covered,
            long[] bestMask, int[] bestCovered) {
        if (covered > bestCovered[0]) {
            bestCovered[0] = covered;
            bestMask[0] = used;
        }
        for (int i = from; i < melds.size(); i++) {
            long meld = melds.get(i);
            if ((meld & used) != 0) continue;
            search(cards, melds, i + 1, used | meld, covered + points(cardsOf(cards, meld)), bestMask, bestCovered);
        }
    }

    /** Recovers the individual melds from the union mask the search kept. */
    private static List<Long> chosenMelds(List<Card> cards, List<Long> melds, long union) {
        List<Long> out = new ArrayList<>();
        pick(melds, 0, union, new ArrayList<>(), out);
        return out;
    }

    private static boolean pick(List<Long> melds, int from, long remaining, List<Long> path, List<Long> out) {
        if (remaining == 0) {
            out.addAll(path);
            return true;
        }
        for (int i = from; i < melds.size(); i++) {
            long meld = melds.get(i);
            if ((meld & remaining) == meld) {
                path.add(meld);
                if (pick(melds, i + 1, remaining & ~meld, path, out)) return true;
                path.remove(path.size() - 1);
            }
        }
        return false;
    }

    /** Every set (3 or 4 of a rank) and every run (3+ in sequence, same suit), as bitmasks. */
    private static List<Long> candidateMelds(List<Card> cards) {
        List<Long> melds = new ArrayList<>();

        Map<Integer, List<Integer>> byRank = new TreeMap<>();
        for (int i = 0; i < cards.size(); i++) byRank.computeIfAbsent(rank(cards.get(i)), r -> new ArrayList<>()).add(i);
        for (List<Integer> same : byRank.values()) {
            if (same.size() == 4) melds.add(mask(same));
            if (same.size() >= 3) {
                for (int skip = 0; skip < same.size(); skip++) {
                    if (same.size() == 3 && skip > 0) break;
                    List<Integer> three = new ArrayList<>(same);
                    if (same.size() == 4) three.remove(skip);
                    melds.add(mask(three));
                }
            }
        }

        Map<Suit, List<Integer>> bySuit = new TreeMap<>();
        for (int i = 0; i < cards.size(); i++) bySuit.computeIfAbsent(cards.get(i).suit(), s -> new ArrayList<>()).add(i);
        for (List<Integer> suited : bySuit.values()) {
            // `cards` is sorted by suit then rank, so each suit's indices are already in rank order.
            for (int start = 0; start < suited.size(); start++) {
                for (int end = start + 2; end < suited.size(); end++) {
                    if (rank(cards.get(suited.get(end))) - rank(cards.get(suited.get(start))) != end - start) break;
                    melds.add(mask(suited.subList(start, end + 1)));
                }
            }
        }
        return melds;
    }

    private static long mask(List<Integer> indices) {
        long mask = 0;
        for (int i : indices) mask |= 1L << i;
        return mask;
    }

    private static List<Card> cardsOf(List<Card> cards, long mask) {
        List<Card> out = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            if ((mask & (1L << i)) != 0) out.add(cards.get(i));
        }
        return out;
    }

    /**
     * Lays the defender's deadwood onto the knocker's melds: a fourth card on a set, or a card
     * extending a run at either end, repeated until nothing more fits. Greedy — it can miss
     * the rare case where rearranging the defender's own melds would free a better layoff.
     *
     * @return the cards laid off; {@code melds} is not modified
     */
    static List<Card> layoffs(List<Card> deadwood, List<List<Card>> melds) {
        List<List<Card>> extended = melds.stream().map(meld -> (List<Card>) new ArrayList<>(meld))
                .collect(Collectors.toList());
        List<Card> remaining = new ArrayList<>(deadwood);
        List<Card> laid = new ArrayList<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Card card : List.copyOf(remaining)) {
                for (List<Card> meld : extended) {
                    if (fits(card, meld)) {
                        meld.add(card);
                        remaining.remove(card);
                        laid.add(card);
                        changed = true;
                        break;
                    }
                }
            }
        }
        return laid;
    }

    private static boolean fits(Card card, List<Card> meld) {
        boolean isSet = meld.stream().map(Melds::rank).distinct().count() == 1;
        if (isSet) return meld.size() < 4 && rank(meld.get(0)) == rank(card);
        if (meld.get(0).suit() != card.suit()) return false;
        int low = meld.stream().mapToInt(Melds::rank).min().orElseThrow();
        int high = meld.stream().mapToInt(Melds::rank).max().orElseThrow();
        return rank(card) == low - 1 || rank(card) == high + 1;
    }
}
