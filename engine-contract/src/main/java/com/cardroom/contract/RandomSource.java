package com.cardroom.contract;

/**
 * The only randomness a game module may use. Seeded per hand and replayable, which is what
 * makes the deal re-derivable from the revealed seed.
 */
@FunctionalInterface
public interface RandomSource {

    /** Uniform in {@code [0, bound)}. Implementations must reject-sample, never modulo-fold. */
    int nextInt(int bound);
}
