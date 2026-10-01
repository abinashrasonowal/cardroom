package com.whitejack.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.whitejack.bots.Decision;
import java.util.List;
import org.junit.jupiter.api.Test;

class BotReasoningStoreTest {

    private final BotReasoningStore store = new BotReasoningStore();

    private static Decision at(int hand) {
        return new Decision(hand, "p1", "Jev 1 (bot)", "play",
                List.of(new Decision.Option("Q♠", 0.7), new Decision.Option("2♣", 0.3)),
                List.of(0), Decision.Source.ADVISOR, 12);
    }

    @Test
    void theLiveHandIsNeverServed() {
        store.handSeen("ROOM01", 0, false);
        store.record("ROOM01", at(0));
        assertTrue(store.finished("ROOM01").isEmpty(), "hand 0 is still being played");

        store.handSeen("ROOM01", 1, false);
        store.record("ROOM01", at(1));
        List<BotReasoningStore.HandNotes> finished = store.finished("ROOM01");
        assertEquals(1, finished.size());
        assertEquals(0, finished.get(0).hand());
    }

    @Test
    void theLastHandIsReleasedWhenTheGameEnds() {
        store.handSeen("ROOM01", 3, false);
        store.record("ROOM01", at(3));
        store.handSeen("ROOM01", 3, true);
        assertEquals(3, store.finished("ROOM01").get(0).hand());
    }

    @Test
    void onlyTheNewestHandsAreKeptAndAForgottenRoomIsEmpty() {
        for (int hand = 0; hand < 12; hand++) {
            store.handSeen("ROOM01", hand, false);
            store.record("ROOM01", at(hand));
        }
        List<BotReasoningStore.HandNotes> finished = store.finished("ROOM01");
        assertEquals(BotReasoningStore.KEEP_HANDS, finished.size());
        assertEquals(10, finished.get(0).hand(), "newest finished hand first");

        store.forget("ROOM01");
        assertTrue(store.finished("ROOM01").isEmpty());
    }

    @Test
    void gamesWithoutHandsKeepNothing() {
        store.record("ROOM01", at(-1));
        store.handSeen("ROOM01", -1, true);
        assertTrue(store.finished("ROOM01").isEmpty());
    }
}
