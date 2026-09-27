package com.cardroom.server.dto.frame;

import com.cardroom.contract.PlayerView;

/** Full view on reconnect, with the sequence number it already reflects. */
public record SyncFrame(int v, String type, long seq, String viewType, PlayerView view) implements ServerFrame {

    public static SyncFrame of(long seq, PlayerView view) {
        return new SyncFrame(PROTOCOL, "sync", seq, ServerFrame.viewType(view), view);
    }
}
