package com.whitejack.server.dto.frame;

/** {@code re} echoes the {@code id} of the intent that was accepted. */
public record AcceptedFrame(int v, String type, String re, long seq) implements ServerFrame {

    public static AcceptedFrame of(String re, long seq) {
        return new AcceptedFrame(PROTOCOL, "accepted", re, seq);
    }
}
