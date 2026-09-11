package com.cardroom.engine;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class FairnessTest {

    private static final List<String> SEEDS = List.of("aaaaaaaa", "bbbbbbbb");

    private static byte[] countingSeed() {
        byte[] seed = new byte[32];
        for (int i = 0; i < 32; i++) seed[i] = (byte) i;
        return seed;
    }

    private static List<Integer> draw(HmacRandom rng, int count, int bound) {
        List<Integer> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) out.add(rng.nextInt(bound));
        return out;
    }

    // ---- SeedCommit ----

    @Test
    void commitMatchesTheRevealedSeed() {
        SeedCommit hand = SeedCommit.fresh();
        assertEquals(32, hand.reveal().length);
        assertTrue(SeedCommit.verify(hand.commit(), hand.reveal()));
    }

    @Test
    void verifyRejectsASeedThatIsNotTheOneCommittedTo() {
        SeedCommit hand = SeedCommit.fresh();
        byte[] tampered = hand.reveal();
        tampered[7] ^= 0x01;
        assertFalse(SeedCommit.verify(hand.commit(), tampered));
    }

    @Test
    void everyHandGetsItsOwnSeed() {
        assertNotEquals(SeedCommit.fresh().commit(), SeedCommit.fresh().commit());
    }

    @Test
    void revealHandsOutACopy() {
        SeedCommit hand = SeedCommit.fresh();
        byte[] first = hand.reveal();
        Arrays.fill(first, (byte) 0);
        assertFalse(Arrays.equals(first, hand.reveal()), "a caller zeroing its copy must not zero the engine's seed");
        assertTrue(SeedCommit.verify(hand.commit(), hand.reveal()));
    }

    @Test
    void toStringNeverPrintsTheSeed() {
        SeedCommit hand = SeedCommit.fresh();
        String printed = hand.toString();
        assertTrue(printed.contains(hand.commit()));
        assertFalse(printed.contains(java.util.HexFormat.of().formatHex(hand.reveal())),
                "one log line before the reveal would hand out the whole deal");
    }

    // ---- HmacRandom ----

    /**
     * Pins the wire format documented on {@link HmacRandom}. These exact values were
     * reproduced independently in ~20 lines of Python (hmac + hashlib), which is the evidence
     * that a browser can re-derive the deal on the /verify page. If this test ever fails, the
     * format changed and every already-published hand became unverifiable.
     */
    @Test
    void knownAnswerVector() {
        assertEquals(
                "630dcd2966c4336691125448bbb25b4ff412a49c732db2c8abc1b8581bd710dd",
                SeedCommit.of(countingSeed()).commit());
        assertEquals(
                List.of(50, 46, 12, 22, 35, 1, 35, 22, 34, 34),
                draw(HmacRandom.forHand(countingSeed(), SEEDS, 0), 10, 52));
    }

    @Test
    void sameInputsDealTheSameCards() {
        assertEquals(
                draw(HmacRandom.forHand(countingSeed(), SEEDS, 0), 200, 52),
                draw(HmacRandom.forHand(countingSeed(), SEEDS, 0), 200, 52));
    }

    @Test
    void aNewHandIndexIsANewDeal() {
        assertNotEquals(
                draw(HmacRandom.forHand(countingSeed(), SEEDS, 0), 50, 52),
                draw(HmacRandom.forHand(countingSeed(), SEEDS, 1), 50, 52));
    }

    @Test
    void clientSeedsSteerTheDeal() {
        assertNotEquals(
                draw(HmacRandom.forHand(countingSeed(), SEEDS, 0), 50, 52),
                draw(HmacRandom.forHand(countingSeed(), List.of("aaaaaaaa", "cccccccc"), 0), 50, 52));
    }

    @Test
    void theSeparatorStopsSeedCollisions() {
        // Without ':' both of these concatenate to "aaaaaaaaaabbbbbbbbbb", so one player could
        // pick a seed that forces a deal they had already simulated.
        assertNotEquals(
                draw(HmacRandom.forHand(countingSeed(), List.of("aaaaaaaaaa", "bbbbbbbbbb"), 0), 20, 52),
                draw(HmacRandom.forHand(countingSeed(), List.of("aaaaaaaaaab", "bbbbbbbbb"), 0), 20, 52));
    }

    @Test
    void malformedClientSeedsAreRejected() {
        for (String bad : List.of("AAAAAAAA", "abc", "zzzzzzzz", "aaaa aaaa", "a".repeat(129))) {
            assertThrows(IllegalArgumentException.class,
                    () -> HmacRandom.forHand(countingSeed(), List.of(bad), 0), "accepted: " + bad);
        }
        assertThrows(IllegalArgumentException.class, () -> HmacRandom.forHand(countingSeed(), SEEDS, -1));
        assertThrows(IllegalArgumentException.class, () -> SeedCommit.of(new byte[16]));
    }

    @Test
    void nextIntStaysInRange() {
        HmacRandom rng = HmacRandom.forHand(countingSeed(), SEEDS, 0);
        for (int bound : List.of(1, 2, 3, 7, 52, 1000, Integer.MAX_VALUE)) {
            for (int i = 0; i < 500; i++) {
                int v = rng.nextInt(bound);
                assertTrue(v >= 0 && v < bound, v + " out of [0," + bound + ")");
            }
        }
        assertThrows(IllegalArgumentException.class, () -> rng.nextInt(0));
        assertThrows(IllegalArgumentException.class, () -> rng.nextInt(-5));
    }

    /**
     * Catches a broken block stream — a counter that never advances, a buffer read twice, a
     * sign-extended byte. It does <em>not</em> prove the rejection sampling: for a 32-bit
     * source and a bound of 6 the modulo bias is about one part in a billion, so no feasible
     * sample size would see it. That line is reviewed, not measured.
     */
    @Test
    void theStreamIsFlat() {
        HmacRandom rng = HmacRandom.forHand(countingSeed(), SEEDS, 0);
        int[] buckets = new int[6];
        int draws = 60_000;
        for (int i = 0; i < draws; i++) buckets[rng.nextInt(6)]++;

        double expected = draws / 6.0;
        double chiSquare = 0;
        for (int count : buckets) chiSquare += Math.pow(count - expected, 2) / expected;
        assertTrue(chiSquare < 30, "chi-square " + chiSquare + " over " + Arrays.toString(buckets));
    }
}
