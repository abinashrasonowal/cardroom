package com.cardroom.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cardroom.contract.GameDefinition;
import com.cardroom.contract.RoomCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RoomRegistryTest {

    /** java.time.Clock with a hand crank — no sleeping in a test about a five minute timeout. */
    private static final class MutableClock extends Clock {
        private volatile Instant now = Instant.parse("2026-09-11T10:00:00Z");

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final TurnClock turnClock = new TurnClock();
    private final FakeGame game = new FakeGame();
    private final RoomRegistry registry = new RoomRegistry(
            id -> game.id().equals(id) ? (GameDefinition<?, ?, ?>) game : null,
            new RecordingBroadcaster(), turnClock, clock);

    @AfterEach
    void tearDown() {
        registry.close();
        turnClock.close();
    }

    @Test
    void codesAreUniqueAndUnambiguousWhenReadAloud() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            codes.add(registry.create(game.id(), Map.of()).code().value());
        }
        assertEquals(500, codes.size(), "the registry handed out a code twice");
        for (String code : codes) {
            assertEquals(6, code.length());
            assertTrue(code.matches("[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{6}"),
                    code + " contains a character that is ambiguous over a phone call");
        }
    }

    @Test
    void aRoomIsFoundByItsCodeRegardlessOfHowItIsTyped() {
        RoomActor actor = registry.create(game.id(), Map.of());
        assertEquals(actor, registry.find(actor.code()).orElseThrow());
        assertEquals(actor, registry.find(new RoomCode(actor.code().value().toLowerCase())).orElseThrow());
        assertTrue(registry.find(new RoomCode("ZZZZZZ")).isEmpty());
    }

    @Test
    void anUnknownGameIsRefusedBeforeAThreadIsStarted() {
        assertThrows(IllegalArgumentException.class, () -> registry.create("pinochle", Map.of()));
        assertEquals(0, registry.size());
    }

    @Test
    void quietRoomsAreReapedAndLiveOnesAreNot() {
        RoomActor stale = registry.create(game.id(), Map.of());
        clock.advance(Duration.ofMinutes(10));
        RoomActor fresh = registry.create(game.id(), Map.of());

        assertEquals(1, registry.reapIdle(Duration.ofMinutes(5)));
        assertTrue(registry.find(stale.code()).isEmpty());
        assertFalse(stale.isOpen());
        assertTrue(registry.find(fresh.code()).isPresent());
    }

    @Test
    void aDeadRoomIsReapedEvenIfItOnlyJustDied() {
        RoomActor actor = registry.create(game.id(), Map.of());
        actor.close(); // as an uncaught game exception would leave it

        assertEquals(1, registry.reapIdle(Duration.ofHours(1)));
        assertTrue(registry.find(actor.code()).isEmpty(), "a room nobody drains must not stay findable");
    }

    @Test
    void closingTheRegistryClosesEveryRoom() {
        RoomActor a = registry.create(game.id(), Map.of());
        RoomActor b = registry.create(game.id(), Map.of());

        registry.close();

        assertEquals(0, registry.size());
        assertFalse(a.isOpen());
        assertFalse(b.isOpen());
    }
}
