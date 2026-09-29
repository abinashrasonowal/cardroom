package com.whitejack.bots;

import java.util.Optional;

/**
 * How to play one game from its projected view. The view is parsed into {@code V} before the
 * brain sees it, so a brain works with typed fields rather than raw JSON.
 *
 * @param <V> the bot-side mirror of the game's {@code PlayerView}
 */
public interface GameBrain<V> {

    /** Matches the server's {@code GameDefinition.id()}. */
    String gameId();

    Class<V> viewType();

    /** Rules and strategy hints for the advisor's system prompt. */
    String rules();

    /** What {@code me} must decide in {@code view}, or empty when it is not their move. */
    Optional<Choice> choose(V view, String me);

    /** True once the bot has nothing left to do at this table. */
    boolean isOver(V view);
}
