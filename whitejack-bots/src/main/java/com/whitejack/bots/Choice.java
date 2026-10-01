package com.whitejack.bots;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * A decision the bot owes the table right now: pick {@code picks} distinct options.
 *
 * <p>This is what makes a bot unable to cheat or blunder into a reject: the advisor only ever
 * ranks {@code options}, every option was read off the server's own legal-move lists, and {@code fallback} is a valid pick when the advisor has nothing useful to say.
 *
 * @param key identifies the decision point, so the same view arriving twice is not acted on twice
 * @param state the position in words, for the advisor
 * @param question what is being decided, for the advisor
 * @param assemble turns the picked options into the intent payload
 */
public record Choice(String key, String state, String question, List<Move> options, int picks, List<Integer> fallback,
        Function<List<Move>, JsonNode> assemble) {

    public Choice {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(question, "question");
        options = List.copyOf(options);
        fallback = List.copyOf(fallback);
        Objects.requireNonNull(assemble, "assemble");
        if (picks < 1 || picks > options.size()) {
            throw new IllegalArgumentException("cannot pick " + picks + " of " + options.size());
        }
        if (!isValid(fallback, options.size(), picks)) {
            throw new IllegalArgumentException("fallback " + fallback + " is not a valid pick");
        }
    }

    /** A single-option decision whose intent is the option itself. */
    public static Choice single(String key, String state, String question, List<Move> options, int fallback) {
        return new Choice(key, state, question, options, 1, List.of(fallback), picked -> picked.get(0).value());
    }

    /** Nothing to ask when there is only one way to do it. */
    public boolean isForced() {
        return options.size() == picks;
    }

    /** The {@code picks} best-ranked options, best first; ties go to the lower index. */
    public List<Integer> best(double[] ranking) {
        if (ranking.length != options.size()) throw new IllegalArgumentException("ranking covers " + ranking.length + " options");
        return IntStream.range(0, ranking.length).boxed()
                .sorted(Comparator.comparingDouble((Integer i) -> ranking[i]).reversed())
                .limit(picks)
                .toList();
    }

    public JsonNode intent(List<Integer> indices) {
        if (!isValid(indices, options.size(), picks)) throw new IllegalArgumentException("invalid pick " + indices);
        return assemble.apply(indices.stream().map(options::get).toList());
    }

    static boolean isValid(List<Integer> indices, int size, int picks) {
        if (indices.size() != picks) return false;
        if (new HashSet<>(indices).size() != picks) return false;
        return indices.stream().allMatch(i -> i >= 0 && i < size);
    }
}
