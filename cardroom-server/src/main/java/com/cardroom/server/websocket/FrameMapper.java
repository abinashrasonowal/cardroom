package com.cardroom.server.websocket;

import com.cardroom.engine.Broadcaster;
import com.cardroom.engine.Snapshot;
import com.cardroom.server.dto.frame.AcceptedFrame;
import com.cardroom.server.dto.frame.FaultFrame;
import com.cardroom.server.dto.frame.RejectedFrame;
import com.cardroom.server.dto.frame.ServerFrame;
import com.cardroom.server.dto.frame.SyncFrame;

/** Engine messages → wire frames. The engine never sees a DTO; the client never sees an engine type. */
final class FrameMapper {

    private FrameMapper() {}

    static ServerFrame toFrame(Object message) {
        if (message instanceof Broadcaster.Accepted a) return AcceptedFrame.of(a.clientMsgId(), a.seq());
        if (message instanceof Broadcaster.Rejected r) return RejectedFrame.of(r.clientMsgId(), r.error(), r.detail());
        if (message instanceof Snapshot s) return SyncFrame.of(s.seq(), s.view());
        if (message instanceof Broadcaster.Fault f) return FaultFrame.of(f.room(), f.detail());
        throw new IllegalArgumentException("no wire form for " + message.getClass().getName());
    }
}
