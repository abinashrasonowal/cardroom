package com.cardroom.contract;

import java.util.List;

/**
 * Everything ambient a game module is allowed to read. Wall-clock time and randomness arrive
 * here and nowhere else — {@code Instant.now()} or {@code new Random()} inside a module
 * destroys replay, and ArchUnit bans both (§15).
 */
public interface EngineContext {

    /** Epoch millis, from a {@code java.time.Clock} the engine owns. */
    long now();

    RandomSource random();

    List<Seat> seats();
}
