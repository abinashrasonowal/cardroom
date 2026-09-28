package com.whitejack.engine;

import com.whitejack.contract.EngineContext;
import com.whitejack.contract.RandomSource;
import com.whitejack.contract.Seat;
import java.time.Clock;
import java.util.List;

/**
 * The ambient state a game module may read, for one hand. The {@link RandomSource} is this
 * hand's {@link HmacRandom}, which is why a new one is built per hand rather than per room.
 */
record RoomContext(Clock clock, RandomSource random, List<Seat> seats) implements EngineContext {

    RoomContext {
        seats = List.copyOf(seats);
    }

    @Override
    public long now() {
        return clock.millis();
    }
}
