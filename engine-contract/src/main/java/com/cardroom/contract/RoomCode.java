package com.cardroom.contract;

import java.util.Objects;

/**
 * Crockford base32, 6 characters. The alphabet excludes I, O, 1 and 0 so a code stays
 * unambiguous when read aloud, and the compact constructor upper-cases so that a code typed
 * as "k7m2qx" joins the same room as "K7M2QX".
 */
public record RoomCode(String value) {
    public RoomCode {
        Objects.requireNonNull(value, "value");
        value = value.toUpperCase();
        if (value.isBlank()) throw new IllegalArgumentException("blank RoomCode");
    }

    @Override
    public String toString() {
        return value;
    }
}
