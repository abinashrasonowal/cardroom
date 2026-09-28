package com.whitejack.server.websocket;

import com.whitejack.engine.Broadcaster;
import com.whitejack.engine.Snapshot;
import com.whitejack.server.dto.frame.AcceptedFrame;
import com.whitejack.server.dto.frame.FaultFrame;
import com.whitejack.server.dto.frame.RejectedFrame;
import com.whitejack.server.dto.frame.ServerFrame;
import com.whitejack.server.dto.frame.SyncFrame;

/** Engine messages → wire frames. The engine never sees a DTO; the client never sees an engine type. */
final class FrameMapper {

    private FrameMapper() {}

    /** @param game the room's game id, stamped on frames that carry a view */
    static ServerFrame toFrame(Object message, String game) {
        if (message instanceof Broadcaster.Accepted a) return AcceptedFrame.of(a.clientMsgId(), a.seq());
        if (message instanceof Broadcaster.Rejected r) return RejectedFrame.of(r.clientMsgId(), r.error(), r.detail());
        if (message instanceof Snapshot s) return SyncFrame.of(s.seq(), game, s.view());
        if (message instanceof Broadcaster.Fault f) return FaultFrame.of(f.room(), f.detail());
        throw new IllegalArgumentException("no wire form for " + message.getClass().getName());
    }
}
