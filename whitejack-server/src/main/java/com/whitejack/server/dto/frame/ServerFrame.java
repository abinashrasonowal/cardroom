package com.whitejack.server.dto.frame;

import com.whitejack.contract.PlayerView;
import com.whitejack.engine.LobbyView;

/** Marker for every server → client frame (§7). Each carries {@code v} and {@code type} first. */
public sealed interface ServerFrame
        permits UpdateFrame, SyncFrame, AcceptedFrame, RejectedFrame, FaultFrame {

    int PROTOCOL = 1;

    /** Lobby and game views share one frame shape; this tells the client which it got. */
    static String viewType(PlayerView view) {
        return view instanceof LobbyView ? "lobby" : "game";
    }
}
