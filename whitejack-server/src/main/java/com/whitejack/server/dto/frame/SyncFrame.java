package com.whitejack.server.dto.frame;

import com.whitejack.contract.PlayerView;

/**
 * Full view on reconnect, with the sequence number it already reflects. {@code game} matters
 * most here: a tab refreshed mid-game gets no lobby view to learn it from.
 */
public record SyncFrame(int v, String type, long seq, String game, String viewType, PlayerView view)
        implements ServerFrame {

    public static SyncFrame of(long seq, String game, PlayerView view) {
        return new SyncFrame(PROTOCOL, "sync", seq, game, ServerFrame.viewType(view), view);
    }
}
