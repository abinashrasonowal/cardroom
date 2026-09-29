package com.whitejack.bots;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The prompt the advisor sees, and reading a move back out of its reply. */
public final class Prompts {

    private static final Pattern MOVE = Pattern.compile("MOVE\\s*:\\s*([0-9][0-9,\\s]*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BARE = Pattern.compile("\\s*([0-9][0-9,\\s]*)\\s*");
    private static final Pattern NUMBER = Pattern.compile("[0-9]+");

    private Prompts() {}

    public static String system(String rules, int picks) {
        String format = picks == 1
                ? "Reply with exactly one line: MOVE: <number>"
                : "Reply with exactly one line: MOVE: <n1>, <n2>, ... listing " + picks + " different numbers";
        return "You are a strong card player seated at an online table. " + rules
                + "\nYou will get the position and a numbered list of your legal options. Choose only from that list. "
                + format + "\nDo not explain.";
    }

    public static String user(Choice choice) {
        StringBuilder text = new StringBuilder(choice.state()).append("\n\nYour options:\n");
        for (int i = 0; i < choice.options().size(); i++) {
            text.append(i + 1).append(". ").append(choice.options().get(i).label()).append('\n');
        }
        if (choice.picks() > 1) text.append("\nPick ").append(choice.picks()).append(" different options.");
        return text.toString();
    }

    /** Zero-based indices, or empty if the reply is not a valid pick for {@code choice}. */
    public static Optional<List<Integer>> parse(String reply, Choice choice) {
        if (reply == null) return Optional.empty();
        Matcher move = MOVE.matcher(reply);
        String numbers = null;
        while (move.find()) numbers = move.group(1); // the last MOVE line wins, after any musing
        if (numbers == null) {
            Matcher bare = BARE.matcher(reply);
            if (!bare.matches()) return Optional.empty();
            numbers = bare.group(1);
        }
        List<Integer> indices = new ArrayList<>();
        Matcher number = NUMBER.matcher(numbers);
        while (number.find()) {
            try {
                indices.add(Integer.parseInt(number.group()) - 1);
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Choice.isValid(indices, choice.options().size(), choice.picks()) ? Optional.of(indices) : Optional.empty();
    }
}
