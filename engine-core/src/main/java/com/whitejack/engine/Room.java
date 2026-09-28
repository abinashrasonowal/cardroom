package com.whitejack.engine;

import com.whitejack.contract.PlayerId;
import com.whitejack.contract.RoomCode;
import com.whitejack.contract.Seat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything about a room that is not the game itself.
 *
 * <p><b>Mutable, and touched only by its own {@link RoomActor} thread.</b> No lock guards it and
 * none is needed: one actor, one writer. Nothing outside the actor may hold a reference.
 *
 * <p>Occupants are kept in seating order, so "longest seated" — which decides who inherits the
 * host role — is just the first match.
 */
final class Room {

    final RoomCode code;
    final String gameId;
    final Map<String, String> options;

    private final LinkedHashMap<PlayerId, Occupant> occupants = new LinkedHashMap<>();

    PlayerId hostId;
    Phase phase = Phase.LOBBY;

    /** One per hand, never per room. Step 6 moves this onto a {@code Hand} record. */
    SeedCommit seed;

    Room(RoomCode code, String gameId, PlayerId hostId, Map<String, String> options) {
        this.code = code;
        this.gameId = gameId;
        this.hostId = hostId;
        this.options = Map.copyOf(options);
    }

    Occupant occupant(PlayerId player) {
        return occupants.get(player);
    }

    void put(Occupant occupant) {
        occupants.put(occupant.seat().id(), occupant);
    }

    void remove(PlayerId player) {
        occupants.remove(player);
    }

    List<Occupant> occupants() {
        return List.copyOf(occupants.values());
    }

    int size() {
        return occupants.size();
    }

    List<Seat> seats() {
        List<Seat> seats = new ArrayList<>(occupants.size());
        for (Occupant occupant : occupants.values()) seats.add(occupant.seat());
        return seats;
    }

    List<String> clientSeeds() {
        List<String> seeds = new ArrayList<>(occupants.size());
        for (Occupant occupant : occupants.values()) seeds.add(occupant.clientSeed());
        return seeds;
    }

    /**
     * Closes the gaps left by players who left the lobby, so seat indices are 0..n-1 when the
     * game starts. Called once at start and never during play, where renumbering would move
     * people out from under a game state that already named their seats.
     */
    void renumberSeats() {
        List<Occupant> ordered = occupants();
        occupants.clear();
        for (int i = 0; i < ordered.size(); i++) put(ordered.get(i).atSeat(i));
    }

    /** §11: the host role transfers to the longest-seated connected player; the room survives. */
    PlayerId longestSeatedConnected() {
        for (Occupant occupant : occupants.values()) {
            if (occupant.connected()) return occupant.seat().id();
        }
        return null;
    }

    LobbyView lobbyView() {
        List<LobbyView.Member> members = new ArrayList<>(occupants.size());
        for (Occupant occupant : occupants.values()) {
            members.add(new LobbyView.Member(
                    occupant.seat().index(), occupant.seat().id(), occupant.seat().nick(), occupant.connected()));
        }
        return new LobbyView(code, gameId, phase, hostId, members);
    }
}
