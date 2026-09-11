package com.cardroom.contract;

import java.time.Duration;
import java.util.Objects;

/** Who is on the clock and for how long. Limits are per game: blackjack ~15s, rummy ~45s. */
public record Turn(PlayerId actor, Duration limit) {
    public Turn {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(limit, "limit");
        if (limit.isNegative() || limit.isZero()) {
            throw new IllegalArgumentException("turn limit must be positive: " + limit);
        }
    }
}
