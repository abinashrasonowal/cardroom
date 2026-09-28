package com.whitejack.server.dto.frame;

import com.whitejack.contract.RoomCode;

/** The room died of an uncaught exception; sent so nobody waits forever. */
public record FaultFrame(int v, String type, RoomCode room, String detail) implements ServerFrame {

    public static FaultFrame of(RoomCode room, String detail) {
        return new FaultFrame(PROTOCOL, "fault", room, detail);
    }
}
