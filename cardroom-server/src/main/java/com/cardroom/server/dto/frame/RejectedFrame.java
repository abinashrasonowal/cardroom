package com.cardroom.server.dto.frame;

import com.cardroom.contract.ErrorCode;

/** {@code error} is a closed-enum code, {@code detail} is for humans; {@code re} may be null. */
public record RejectedFrame(int v, String type, String re, ErrorCode error, String detail) implements ServerFrame {

    public static RejectedFrame of(String re, ErrorCode error, String detail) {
        return new RejectedFrame(PROTOCOL, "rejected", re, error, detail);
    }
}
