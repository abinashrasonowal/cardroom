package com.cardroom.engine;

import java.util.Objects;

/**
 * One WebSocket connection. Lives here and not in the contract: §3 forbids a game module from
 * touching sockets, so it must not be able to name one.
 */
public record SocketId(String value) {
    public SocketId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) throw new IllegalArgumentException("blank SocketId");
    }
}
