package com.whitejack.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.whitejack.contract.EngineContext;
import com.whitejack.contract.ErrorCode;
import com.whitejack.contract.PlayerId;
import com.whitejack.contract.RandomSource;
import com.whitejack.contract.Seat;
import com.whitejack.contract.Validation.Reject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GameSessionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final PlayerId ABI = new PlayerId("abi");
    private static final List<Seat> SEATS = List.of(new Seat(0, ABI, "abi"));
    private static final long NOW = 1_700_000_000_000L;

    /** No custom Clock type: java.time.Clock already ships the test double. */
    private static final EngineContext CTX = new EngineContext() {
        private final Clock clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC);

        @Override
        public long now() {
            return clock.millis();
        }

        @Override
        public RandomSource random() {
            return bound -> 0;
        }

        @Override
        public List<Seat> seats() {
            return SEATS;
        }
    };

    private static GameSession<FakeGame.State, FakeGame.Bump, FakeGame.Bumped> session(FakeGame game) {
        return GameSession.start(game, SEATS, Map.of(), CTX);
    }

    private static JsonNode bump(int events) {
        return MAPPER.createObjectNode().put("type", "bump").put("events", events);
    }

    @Test
    void aFreshSessionHasNothingToReplay() {
        var session = session(new FakeGame());
        assertEquals(-1, session.seq(), "seq is -1 until the first event exists");
        assertEquals(0, session.log().size());
        assertEquals(List.of(), session.state().applied());
    }

    @Test
    void anAcceptedIntentFoldsAppendsAndSwaps() {
        var session = session(new FakeGame());
        Outcome outcome = session.submit(bump(2), ABI);

        assertTrue(outcome.accepted());
        assertEquals(1, outcome.seq(), "two events, so the last sequence number is 1");
        assertEquals(List.of("abi-e0", "abi-e1"), session.state().applied());
        assertEquals(2, session.log().size());
    }

    /** Engine invariant 5, and §9's "rejection is a total no-op". */
    @Test
    void anIllegalIntentChangesNothingAtAll() {
        var session = session(new FakeGame());
        session.submit(bump(2), ABI);

        var stateBefore = session.state();
        long seqBefore = session.seq();
        var logBefore = session.log().all();

        Outcome outcome = session.submit(bump(-1), ABI);

        assertFalse(outcome.accepted());
        assertEquals(ErrorCode.ILLEGAL_MOVE, ((Reject) outcome.result()).code());
        assertEquals(seqBefore, outcome.seq(), "a rejection must not bump the sequence");
        assertSame(stateBefore, session.state(), "state was replaced despite a rejection");
        assertEquals(logBefore, session.log().all());
    }

    /**
     * A malformed frame is client input, not a server fault. If parseIntent's exception escaped,
     * one junk message from any connected socket would close the room for everyone.
     */
    @Test
    void aMalformedIntentCostsTheCallerAnErrorAndNotTheRoom() {
        var session = session(new FakeGame());
        Outcome outcome = session.submit(MAPPER.createObjectNode().put("type", "nonsense"), ABI);

        assertEquals(ErrorCode.MALFORMED_INTENT, ((Reject) outcome.result()).code());
        assertEquals(-1, session.seq());
        assertTrue(session.submit(bump(1), ABI).accepted(), "the session still works afterwards");
    }

    /**
     * The reason §9 says fold first. Appending as we go would leave two events on the log that
     * the state does not reflect, and every later replay would diverge.
     */
    @Test
    void nothingIsAppendedWhenApplyThrowsPartWayThrough() {
        var session = session(new FakeGame("abi-e2"));
        session.submit(bump(2), ABI); // a clean intent first, so we can prove only IT survives

        var stateBefore = session.state();
        long seqBefore = session.seq();

        assertThrows(IllegalStateException.class, () -> session.submit(bump(5), ABI));

        assertEquals(seqBefore, session.seq(), "a half-applied intent appended to the log");
        assertSame(stateBefore, session.state(), "a half-applied intent swapped the state");
        assertEquals(2, session.log().size());
    }

    @Test
    void aGameThatFloodsEventsIsStopped() {
        var session = session(new FakeGame());
        assertThrows(IllegalStateException.class, () -> session.submit(bump(300), ABI));
        assertEquals(-1, session.seq());
    }

    @Test
    void everyEventCarriesItsSequenceClockAndRulesVersion() {
        var session = session(new FakeGame());
        session.submit(bump(3), ABI);

        List<SequencedEvent<FakeGame.Bumped>> events = session.log().all();
        for (int i = 0; i < events.size(); i++) {
            assertEquals(i, events.get(i).seq());
            assertEquals(NOW, events.get(i).at(), "timestamps must come from EngineContext");
            assertEquals(7, events.get(i).gameVersion());
        }
    }

    @Test
    void replayRebuildsExactlyTheLiveState() {
        FakeGame game = new FakeGame();
        var session = session(game);
        session.submit(bump(2), ABI);
        session.submit(bump(1), ABI);

        var rebuilt = session.log().replay(new FakeGame.State(SEATS, List.of()), game);
        assertEquals(session.state(), rebuilt);
    }

    @Test
    void sinceReturnsOnlyWhatAClientMissed() {
        var session = session(new FakeGame());
        session.submit(bump(2), ABI);
        session.submit(bump(2), ABI);

        assertEquals(4, session.log().since(-1).size(), "since(-1) is the whole log");
        assertEquals(List.of("abi-e0", "abi-e1"),
                session.log().since(1).stream().map(e -> e.event().note()).toList());
        assertEquals(List.of(), session.log().since(3));
        assertEquals(List.of(), session.log().since(99));
    }

    @Test
    void logCopiesCannotBeUsedToEditHistory() {
        var session = session(new FakeGame());
        session.submit(bump(1), ABI);
        assertThrows(UnsupportedOperationException.class, () -> session.log().all().clear());
        assertThrows(UnsupportedOperationException.class, () -> session.log().since(-1).clear());
    }

    @Test
    void viewAndClockDelegateToTheDefinition() {
        var session = session(new FakeGame());
        session.submit(bump(1), ABI);

        assertEquals(new FakeGame.View(List.of("abi-e0"), true), session.view(Optional.of(ABI)));
        assertEquals(new FakeGame.View(List.of("abi-e0"), false), session.view(Optional.empty()));
        assertEquals(ABI, session.turn().orElseThrow().actor());
        assertFalse(session.isHandComplete());

        session.submit(bump(2), ABI);
        assertTrue(session.isHandComplete(), "three events end the fake game's hand");
        assertFalse(session.isComplete());
    }
}
