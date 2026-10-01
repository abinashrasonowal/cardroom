package com.whitejack.bots;

import java.util.List;

/**
 * One move a bot sent, and why: every option it weighed with the advisor's probability, the
 * ones it picked, and where the pick came from. Built for showing the reasoning <em>after</em>
 * the hand — the option labels name the bot's own cards, so they are private while it plays.
 *
 * @param hand the game's hand number when the move was made, or -1 for games without hands
 * @param phase the kind of decision, e.g. {@code play}, {@code pass}, {@code draw}, {@code act}
 * @param picked indices into {@code options}, best first
 * @param millis how long the advisor took; 0 when it was not asked
 */
public record Decision(int hand, String player, String nick, String phase, List<Option> options,
        List<Integer> picked, Source source, long millis) {

    /** {@code probability} is null when the advisor gave none (a heuristic or forced move). */
    public record Option(String label, Double probability) {}

    public enum Source {
        /** The advisor ranked the options and the bot played its best. */
        ADVISOR,
        /** Only one legal move, so nothing was asked. */
        FORCED,
        /** The advisor was slow, unreachable or not configured; the brain's heuristic played. */
        HEURISTIC,
        /** The server rejected the advisor's move once; the heuristic played the retry. */
        AFTER_REJECT
    }

    public Decision {
        options = List.copyOf(options);
        picked = List.copyOf(picked);
    }
}
