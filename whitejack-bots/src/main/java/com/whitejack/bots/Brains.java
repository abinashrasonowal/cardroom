package com.whitejack.bots;

import com.whitejack.bots.brain.GinBrain;
import com.whitejack.bots.brain.HeartsBrain;
import com.whitejack.bots.brain.HighCardBrain;
import com.whitejack.bots.brain.PokerBrain;
import java.util.List;
import java.util.Optional;

/** The games a bot knows how to play. */
public final class Brains {

    private static final List<GameBrain<?>> ALL = List.of(new HighCardBrain(), new HeartsBrain(), new GinBrain(),
            new PokerBrain());

    private Brains() {}

    public static Optional<GameBrain<?>> forGame(String gameId) {
        return ALL.stream().filter(brain -> brain.gameId().equals(gameId)).findFirst();
    }
}
