package com.cardroom.contract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Fisher-Yates, the only shuffle in the system. Same {@link RandomSource} in, same order out. */
public final class Shuffler {

    private Shuffler() {}

    public static <T> List<T> shuffle(List<T> items, RandomSource rng) {
        List<T> copy = new ArrayList<>(items);
        for (int i = copy.size() - 1; i > 0; i--) {
            Collections.swap(copy, i, rng.nextInt(i + 1));
        }
        return List.copyOf(copy);
    }
}
