package com.cardroom.contract;

import java.util.Objects;

/** Result of asking a game whether something is allowed. A {@link Reject} is a total no-op (§9). */
public sealed interface Validation {

    /** Singleton: {@code Ok} carries no information, so there is no reason to allocate one per call. */
    Validation OK = new Ok();

    record Ok() implements Validation {}

    record Reject(ErrorCode code, String detail) implements Validation {
        public Reject {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(detail, "detail");
        }
    }

    static Validation reject(ErrorCode code, String detail) {
        return new Reject(code, detail);
    }

    default boolean isOk() {
        return this instanceof Ok;
    }
}
