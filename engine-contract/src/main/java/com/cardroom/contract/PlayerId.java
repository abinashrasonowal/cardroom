package com.cardroom.contract;

import java.util.Objects;

/** Identity without an account: derived from a signed cookie, stable across tabs and refreshes. */
public record PlayerId(String value) {
    public PlayerId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) throw new IllegalArgumentException("blank PlayerId");
    }

    @Override
    public String toString() {
        return value;
    }
}
