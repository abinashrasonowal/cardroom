package com.cardroom.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** High Card arrives as testRuntimeOnly: discovered, never compiled against. */
class GameCatalogTest {

    private final GameCatalog catalog = GameCatalog.load();

    @Test
    void discoversHighCardThroughServiceLoader() {
        assertEquals("high-card", catalog.require("high-card").id());
    }

    @Test
    void unknownIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> catalog.require("no-such-game"));
    }
}
