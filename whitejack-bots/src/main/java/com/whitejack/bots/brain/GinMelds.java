package com.whitejack.bots.brain;

import com.whitejack.contract.Card;
import com.whitejack.contract.Rank;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The least deadwood a gin hand can leave. A bot may not reach into {@code games/gin-rummy}, so
 * this is its own search: every set (3 or 4 of a rank) and run (3+ in suit, aces low) as a bitmask
 * over the hand, then the disjoint combination covering the most points. Hands are 10 or 11
 * cards, so the search is tiny.
 */
final class GinMelds {

    private GinMelds() {}

    /** Deadwood points: ace 1, number cards their face, J Q K 10. */
    static int points(Card card) {
        return Math.min(low(card), 10);
    }

    static int minDeadwood(List<Card> hand) {
        int total = hand.stream().mapToInt(GinMelds::points).sum();
        return total - bestCover(hand, melds(hand), 0, 0, new HashMap<>());
    }

    /** The cards left unmelded by one least-deadwood arrangement. */
    static List<Card> deadwood(List<Card> hand) {
        List<Integer> melds = melds(hand);
        int used = cover(hand, melds);
        List<Card> out = new ArrayList<>();
        for (int i = 0; i < hand.size(); i++) if ((used & 1 << i) == 0) out.add(hand.get(i));
        return out;
    }

    /** The lowest deadwood left after throwing away one card of an 11-card hand, never {@code keep}. */
    static int afterBestDiscard(List<Card> hand, Card keep) {
        int best = Integer.MAX_VALUE;
        for (Card card : hand) {
            if (card.equals(keep)) continue;
            List<Card> rest = new ArrayList<>(hand);
            rest.remove(card);
            best = Math.min(best, minDeadwood(rest));
        }
        return best;
    }

    private static int low(Card card) {
        return card.rank() == Rank.ACE ? 1 : card.rank().value();
    }

    private static List<Integer> melds(List<Card> hand) {
        List<Integer> melds = new ArrayList<>();
        int n = hand.size();
        // Sets: every 3- and 4-card group of one rank.
        for (int a = 0; a < n; a++) {
            for (int b = a + 1; b < n; b++) {
                if (hand.get(a).rank() != hand.get(b).rank()) continue;
                for (int c = b + 1; c < n; c++) {
                    if (hand.get(c).rank() != hand.get(a).rank()) continue;
                    melds.add(1 << a | 1 << b | 1 << c);
                    for (int d = c + 1; d < n; d++) {
                        if (hand.get(d).rank() == hand.get(a).rank()) melds.add(1 << a | 1 << b | 1 << c | 1 << d);
                    }
                }
            }
        }
        // Runs: every stretch of 3+ consecutive ranks in one suit.
        for (int start = 0; start < n; start++) {
            int mask = 1 << start;
            Card from = hand.get(start);
            int next = low(from) + 1;
            while (true) {
                int found = -1;
                for (int i = 0; i < n; i++) {
                    if (hand.get(i).suit() == from.suit() && low(hand.get(i)) == next) found = i;
                }
                if (found < 0) break;
                mask |= 1 << found;
                if (Integer.bitCount(mask) >= 3) melds.add(mask);
                next++;
            }
        }
        return melds;
    }

    /** Most points coverable by disjoint melds from {@code from} on, avoiding {@code used}. */
    private static int bestCover(List<Card> hand, List<Integer> melds, int from, int used, Map<Long, Integer> memo) {
        long key = (long) used << 16 | from;
        Integer cached = memo.get(key);
        if (cached != null) return cached;
        int best = 0;
        for (int m = from; m < melds.size(); m++) {
            int meld = melds.get(m);
            if ((meld & used) != 0) continue;
            best = Math.max(best, value(hand, meld) + bestCover(hand, melds, m + 1, used | meld, memo));
        }
        memo.put(key, best);
        return best;
    }

    /** The mask of melded cards in one best arrangement. */
    private static int cover(List<Card> hand, List<Integer> melds) {
        Map<Long, Integer> memo = new HashMap<>();
        int used = 0;
        int from = 0;
        int remaining = bestCover(hand, melds, 0, 0, memo);
        while (remaining > 0) {
            for (int m = from; m < melds.size(); m++) {
                int meld = melds.get(m);
                if ((meld & used) != 0) continue;
                int rest = bestCover(hand, melds, m + 1, used | meld, memo);
                if (value(hand, meld) + rest == remaining) {
                    used |= meld;
                    from = m + 1;
                    remaining = rest;
                    break;
                }
            }
        }
        return used;
    }

    private static int value(List<Card> hand, int mask) {
        int sum = 0;
        for (int i = 0; i < hand.size(); i++) if ((mask & 1 << i) != 0) sum += points(hand.get(i));
        return sum;
    }
}
