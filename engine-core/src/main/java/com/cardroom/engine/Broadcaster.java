package com.cardroom.engine;

import com.cardroom.contract.ErrorCode;
import com.cardroom.contract.PlayerId;
import com.cardroom.contract.PlayerView;
import com.cardroom.contract.RoomCode;
import java.util.Map;

/**
 * The engine's outbound port, implemented in {@code :server}. engine-core declares it and never
 * imports a socket library — that inversion is what makes the whole engine testable with no I/O.
 *
 * <p>Views go over the wire, never events: an event like {@code Drew(seat=2, A♠)} carries hidden
 * information by construction, so shipping the event stream would need a second redaction filter
 * beside {@code project}. One filter on one path is the only version with no leak (§7).
 */
public interface Broadcaster {

    /** One freshly projected view per viewer. */
    void toRoom(RoomCode code, Map<PlayerId, PlayerView> views);

    /** A reply addressed to one player: an acceptance, a rejection, or a {@link Fault}. */
    void toPlayer(PlayerId player, Object message);

    /** Correlates with the {@code id} of the intent that caused it (§7). */
    record Accepted(String clientMsgId, long seq) {}

    /** {@code error} is a code from a closed enum; {@code detail} is for humans. */
    record Rejected(String clientMsgId, ErrorCode error, String detail) {}

    /** Sent to everyone when a room dies of an uncaught exception, so nobody waits forever. */
    record Fault(RoomCode room, String detail) {}
}
