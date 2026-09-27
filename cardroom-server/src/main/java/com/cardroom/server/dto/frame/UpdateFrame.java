package com.cardroom.server.dto.frame;

import com.cardroom.contract.PlayerView;
import com.cardroom.contract.RoomCode;

/** A freshly projected view after any change in the room. */
public record UpdateFrame(int v, String type, RoomCode room, String viewType, PlayerView view)
        implements ServerFrame {

    public static UpdateFrame of(RoomCode room, PlayerView view) {
        return new UpdateFrame(PROTOCOL, "update", room, ServerFrame.viewType(view), view);
    }
}
