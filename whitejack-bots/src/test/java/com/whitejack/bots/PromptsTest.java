package com.whitejack.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PromptsTest {

    private static Choice choice(int options, int picks) {
        List<Move> moves = java.util.stream.IntStream.range(0, options)
                .mapToObj(i -> new Move("option " + i, JsonNodeFactory.instance.numberNode(i)))
                .toList();
        List<Integer> fallback = java.util.stream.IntStream.range(0, picks).boxed().toList();
        return new Choice("k", "state", moves, picks, fallback, picked -> picked.get(0).value());
    }

    @Test
    void readsAOneBasedMove() {
        assertEquals(Optional.of(List.of(2)), Prompts.parse("MOVE: 3", choice(5, 1)));
        assertEquals(Optional.of(List.of(0)), Prompts.parse("  1 ", choice(5, 1)));
        assertEquals(Optional.of(List.of(4)), Prompts.parse("I think... move: 2\nActually MOVE: 5", choice(5, 1)));
    }

    @Test
    void readsSeveralDistinctPicks() {
        assertEquals(Optional.of(List.of(0, 3, 6)), Prompts.parse("MOVE: 1, 4, 7", choice(13, 3)));
    }

    @Test
    void refusesAnythingThatIsNotALegalPick() {
        Choice one = choice(3, 1);
        assertTrue(Prompts.parse("MOVE: 4", one).isEmpty(), "out of range");
        assertTrue(Prompts.parse("MOVE: 0", one).isEmpty(), "numbering starts at 1");
        assertTrue(Prompts.parse("play the queen", one).isEmpty(), "no number");
        assertTrue(Prompts.parse("MOVE: 99999999999999", one).isEmpty(), "overflow");
        assertTrue(Prompts.parse(null, one).isEmpty());
        Choice three = choice(13, 3);
        assertTrue(Prompts.parse("MOVE: 1, 1, 2", three).isEmpty(), "duplicates");
        assertTrue(Prompts.parse("MOVE: 1, 2", three).isEmpty(), "too few");
    }

    @Test
    void userPromptNumbersEveryOption() {
        String user = Prompts.user(choice(2, 1));
        assertTrue(user.contains("1. option 0"));
        assertTrue(user.contains("2. option 1"));
    }
}
