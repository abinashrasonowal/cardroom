package com.whitejack.engine;

import com.whitejack.contract.Seat;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A seated player as the <em>room</em> sees them: their seat, the seed they contributed, and
 * every socket they currently hold.
 *
 * <p><b>Sockets are a set, not a boolean.</b> One cookie is one identity across tabs, so
 * closing one of three tabs must not start the turn timer on someone who is still sitting
 * there watching.
 */
public record Occupant(Seat seat, String clientSeed, Set<SocketId> sockets) {

    public Occupant {
        Objects.requireNonNull(seat, "seat");
        Objects.requireNonNull(clientSeed, "clientSeed");
        sockets = Set.copyOf(sockets);
    }

    public boolean connected() {
        return !sockets.isEmpty();
    }

    Occupant with(SocketId socket) {
        Set<SocketId> next = new HashSet<>(sockets);
        next.add(socket);
        return new Occupant(seat, clientSeed, next);
    }

    Occupant without(SocketId socket) {
        Set<SocketId> next = new HashSet<>(sockets);
        next.remove(socket);
        return new Occupant(seat, clientSeed, next);
    }

    Occupant atSeat(int index) {
        return new Occupant(new Seat(index, seat.id(), seat.nick()), clientSeed, sockets);
    }
}
