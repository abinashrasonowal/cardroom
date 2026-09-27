package com.cardroom.server.dto.frame;

import com.cardroom.contract.PlayerView;
import com.cardroom.contract.RoomCode;

/** A freshly projected view after any change in the room. {@code game} says which table renders it. */
public record UpdateFrame(int v, String type, RoomCode room, String game, String viewType, PlayerView view)
        implements ServerFrame {

    public static UpdateFrame of(RoomCode room, String game, PlayerView view) {
        return new UpdateFrame(PROTOCOL, "update", room, game, ServerFrame.viewType(view), view);
    }
}
