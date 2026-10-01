package com.whitejack.bots.brain;

import com.whitejack.contract.Card;
import com.whitejack.contract.Rank;
import java.util.List;
import java.util.Random;

/**
 * Hand strength for the poker bot. A bot may not reach into {@code games/poker}, so this is its
 * own evaluator: best five of up to seven cards as one comparable score, and a Monte Carlo
 * equity against random hands. Cards are ints ({@code suit * 13 + rank - 2}) to keep the
 * simulation allocation-free.
 */
final class PokerOdds {

    static final String[] CATEGORIES = {"high card", "pair", "two pair", "three of a kind", "straight", "flush",
            "full house", "four of a kind", "straight flush"};

    private PokerOdds() {}

    /** The category index (into {@link #CATEGORIES}) of a score from {@link #score}. */
    static int category(int score) {
        return score >>> 20;
    }

    /** "pair of jacks", "flush, ace high" — the hand in words. */
    static String describe(List<Card> cards) {
        int score = score(codes(cards), cards.size());
        int top = (score >>> 16) & 0xF;
        String rank = name(top).toLowerCase();
        return switch (category(score)) {
            case 0 -> rank + " high";
            case 1 -> "pair of " + plural(top);
            case 2 -> "two pair, " + plural(top) + " and " + plural((score >>> 12) & 0xF);
            case 3 -> "three " + plural(top);
            case 4 -> "straight, " + rank + " high";
            case 5 -> "flush, " + rank + " high";
            case 6 -> "full house, " + plural(top) + " full of " + plural((score >>> 12) & 0xF);
            case 7 -> "four " + plural(top);
            default -> "straight flush, " + rank + " high";
        };
    }

    static int score(List<Card> cards) {
        return score(codes(cards), cards.size());
    }

    /**
     * Share of the pot {@code mine} wins against {@code opponents} random hands, with the rest of
     * the board dealt at random; ties split.
     */
    static double equity(List<Card> mine, List<Card> board, int opponents, int iterations, Random random) {
        if (mine.size() != 2 || opponents < 1) return 0;
        int[] known = codes(mine, board);
        boolean[] used = new boolean[52];
        for (int code : known) used[code] = true;
        int[] deck = new int[52 - known.length];
        for (int code = 0, i = 0; code < 52; code++) if (!used[code]) deck[i++] = code;

        int missing = 5 - board.size();
        int need = missing + 2 * opponents;
        int[] hand = new int[7];
        double won = 0;
        for (int it = 0; it < iterations; it++) {
            for (int i = 0; i < need; i++) { // partial Fisher-Yates: deck[0..need) is the deal
                int j = i + random.nextInt(deck.length - i);
                int t = deck[i];
                deck[i] = deck[j];
                deck[j] = t;
            }
            for (int i = 0; i < board.size(); i++) hand[2 + i] = known[2 + i];
            for (int i = 0; i < missing; i++) hand[2 + board.size() + i] = deck[i];
            hand[0] = known[0];
            hand[1] = known[1];
            int me = score(hand, 7);
            int best = 0;
            int ties = 0;
            for (int o = 0; o < opponents; o++) {
                hand[0] = deck[missing + 2 * o];
                hand[1] = deck[missing + 2 * o + 1];
                int them = score(hand, 7);
                if (them > best) {
                    best = them;
                    ties = 0;
                }
                if (them == best) ties++;
            }
            if (me > best) won += 1;
            else if (me == best) won += 1.0 / (ties + 1);
        }
        return won / iterations;
    }

    /**
     * Category in bits 20+, then up to five tie-break ranks (2..14) four bits each, highest first.
     * Works for any 1..7 cards.
     */
    static int score(int[] cards, int n) {
        int[] rankCount = new int[15];
        int[] suitCount = new int[4];
        int[] suitMask = new int[4];
        int mask = 0;
        for (int i = 0; i < n; i++) {
            int rank = cards[i] % 13 + 2;
            int suit = cards[i] / 13;
            rankCount[rank]++;
            suitCount[suit]++;
            suitMask[suit] |= 1 << rank;
            mask |= 1 << rank;
        }
        for (int suit = 0; suit < 4; suit++) {
            if (suitCount[suit] < 5) continue;
            int straight = straightTop(suitMask[suit]);
            if (straight > 0) return make(8, straight);
            return make(5, topRanks(suitMask[suit], 5));
        }
        int quad = 0, trip = 0, pair1 = 0, pair2 = 0;
        for (int rank = 14; rank >= 2; rank--) {
            int c = rankCount[rank];
            if (c == 4 && quad == 0) quad = rank;
            else if (c >= 3 && trip == 0) trip = rank;
            else if (c >= 2) { // a second set of trips counts as a pair for a full house
                if (pair1 == 0) pair1 = rank;
                else if (pair2 == 0) pair2 = rank;
            }
        }
        if (quad > 0) return make(7, quad << 4 | topRanks(mask & ~(1 << quad), 1));
        if (trip > 0 && pair1 > 0) return make(6, trip << 4 | pair1);
        int straight = straightTop(mask);
        if (straight > 0) return make(4, straight);
        if (trip > 0) return make(3, trip << 8 | topRanks(mask & ~(1 << trip), 2));
        if (pair2 > 0) return make(2, pair1 << 8 | pair2 << 4 | topRanks(mask & ~(1 << pair1) & ~(1 << pair2), 1));
        if (pair1 > 0) return make(1, pair1 << 12 | topRanks(mask & ~(1 << pair1), 3));
        return make(0, topRanks(mask, 5));
    }

    /** Left-aligns the tie-break nibbles so the first always sits in bits 16..19. */
    private static int make(int category, int ranks) {
        int shifted = ranks;
        while (shifted != 0 && (shifted & 0xF0000) == 0) shifted <<= 4;
        return category << 20 | shifted;
    }

    /** The top rank of the best straight in {@code mask}, 5 for the wheel, 0 for none. */
    private static int straightTop(int mask) {
        int m = mask | ((mask >> 14) & 1) << 1; // an ace also plays low
        for (int top = 14; top >= 5; top--) {
            int run = 0b11111 << (top - 4);
            if ((m & run) == run) return top;
        }
        return 0;
    }

    /** The {@code count} highest ranks in {@code mask}, packed a nibble each, highest first. */
    private static int topRanks(int mask, int count) {
        int packed = 0;
        for (int rank = 14; rank >= 2 && count > 0; rank--) {
            if ((mask & 1 << rank) != 0) {
                packed = packed << 4 | rank;
                count--;
            }
        }
        return packed;
    }

    private static int[] codes(List<Card> cards) {
        return cards.stream().mapToInt(PokerOdds::code).toArray();
    }

    private static int[] codes(List<Card> first, List<Card> second) {
        int[] out = new int[first.size() + second.size()];
        for (int i = 0; i < first.size(); i++) out[i] = code(first.get(i));
        for (int i = 0; i < second.size(); i++) out[first.size() + i] = code(second.get(i));
        return out;
    }

    static int code(Card card) {
        return card.suit().ordinal() * 13 + card.rank().value() - 2;
    }

    private static String name(int value) {
        for (Rank rank : Rank.values()) {
            if (rank.value() == value) return rank.name().charAt(0) + rank.name().substring(1).toLowerCase();
        }
        return "?";
    }

    private static String plural(int value) {
        String name = name(value).toLowerCase();
        return name.equals("six") ? "sixes" : name + "s";
    }
}
