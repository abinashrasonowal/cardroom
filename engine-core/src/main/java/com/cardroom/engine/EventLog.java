package com.cardroom.engine;

import com.cardroom.contract.GameDefinition;
import com.cardroom.contract.GameEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The append-only history of one room. In memory today, designed to be persistable unchanged:
 * {@code EventStore} (§17) writes these records as they are.
 *
 * <p>{@code E} is bound by the log, so {@link #replay} folds without a cast.
 */
public final class EventLog<E extends GameEvent> {

    // ponytail: CopyOnWriteArrayList copies the backing array on every append -- O(n) per event,
    // irrelevant for a hand of tens of events, and it makes since()/replay() safe to run off the
    // room thread with no lock at all. Swap for a synchronized ArrayList if a game ever logs
    // thousands of events in a single hand.
    private final List<SequencedEvent<E>> events = new CopyOnWriteArrayList<>();

    /** Appends and returns the sequence number assigned. Called only from the room thread. */
    public long append(E event, long at, int gameVersion) {
        long seq = events.size();
        events.add(new SequencedEvent<>(seq, at, gameVersion, event));
        return seq;
    }

    /** The last assigned sequence number, or -1 before anything has happened. */
    public long seq() {
        return events.size() - 1L;
    }

    public int size() {
        return events.size();
    }

    public List<SequencedEvent<E>> all() {
        return List.copyOf(events);
    }

    /** Everything strictly after {@code seq}. {@code since(-1)} is the whole log. */
    public List<SequencedEvent<E>> since(long seq) {
        int from = (int) Math.max(0, seq + 1);
        if (from >= events.size()) return List.of();
        return List.copyOf(new ArrayList<>(events.subList(from, events.size())));
    }

    /**
     * Folds the whole log over a starting state. Proves {@code apply} is deterministic — which
     * is strictly less than proving the deal was fair: a rigged shuffle replays perfectly. The
     * real claim needs the deal re-derived from the revealed seed (§12).
     */
    public <S> S replay(S initial, GameDefinition<S, ?, E> def) {
        S state = initial;
        for (SequencedEvent<E> sequenced : events) {
            state = def.apply(state, sequenced.event());
        }
        return state;
    }
}
