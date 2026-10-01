package com.whitejack.bots;

import java.util.Optional;

/**
 * A model the bot may consult. An empty answer — no key, a timeout, an outage — is normal,
 * not an error: the bot then plays its heuristic move.
 */
public interface Advisor {

    /**
     * How good each of {@code choice.options()} is, by index: higher is better. The bot takes
     * the {@code choice.picks()} best, so a model that picks one option can still rank three.
     *
     * @param rules the game's rules, as context
     */
    Optional<double[]> rank(String rules, Choice choice);

    /** Heuristic play only. */
    Advisor NONE = (rules, choice) -> Optional.empty();
}
