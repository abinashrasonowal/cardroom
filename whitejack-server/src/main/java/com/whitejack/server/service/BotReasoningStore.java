package com.whitejack.server.service;

import com.whitejack.bots.Decision;
import com.whitejack.bots.ReasoningLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * What the bots in each room decided, released one hand late. A bot's options are its own
 * cards (Hearts, Gin) or follow from them (Poker odds), so a hand's decisions are served only
 * once some bot has seen a later hand or the game has ended — never while that hand is live.
 *
 * <p>Memory is bounded: the last {@link #KEEP_HANDS} hands per room, {@link #MAX_PER_HAND}
 * decisions each, and a room is forgotten when its bots are swept.
 */
@Component
public class BotReasoningStore implements ReasoningLog {

    static final int KEEP_HANDS = 5;
    static final int MAX_PER_HAND = 200;

    /** One finished hand's decisions, in the order they were sent. */
    public record HandNotes(int hand, List<Decision> decisions) {}

    private static final class RoomNotes {
        final TreeMap<Integer, List<Decision>> hands = new TreeMap<>();
        int newestHand = -1;
        boolean over;
    }

    private final Map<String, RoomNotes> rooms = new ConcurrentHashMap<>();

    @Override
    public void record(String room, Decision decision) {
        if (decision.hand() < 0) return; // a game without hands has nothing to release later
        RoomNotes notes = rooms.computeIfAbsent(room, r -> new RoomNotes());
        synchronized (notes) {
            List<Decision> hand = notes.hands.computeIfAbsent(decision.hand(), h -> new ArrayList<>());
            if (hand.size() < MAX_PER_HAND) hand.add(decision);
            // Keep the live hand plus the finished ones anyone may still ask about.
            while (notes.hands.size() > KEEP_HANDS + 1) notes.hands.pollFirstEntry();
        }
    }

    @Override
    public void handSeen(String room, int hand, boolean over) {
        RoomNotes notes = rooms.computeIfAbsent(room, r -> new RoomNotes());
        synchronized (notes) {
            notes.newestHand = Math.max(notes.newestHand, hand);
            notes.over |= over;
        }
    }

    /** Finished hands only, newest first. */
    public List<HandNotes> finished(String room) {
        RoomNotes notes = rooms.get(room);
        if (notes == null) return List.of();
        synchronized (notes) {
            List<HandNotes> out = new ArrayList<>();
            notes.hands.descendingMap().forEach((hand, decisions) -> {
                if (notes.over || hand < notes.newestHand) out.add(new HandNotes(hand, List.copyOf(decisions)));
            });
            return out.size() > KEEP_HANDS ? out.subList(0, KEEP_HANDS) : out;
        }
    }

    public void forget(String room) {
        rooms.remove(room);
    }
}
