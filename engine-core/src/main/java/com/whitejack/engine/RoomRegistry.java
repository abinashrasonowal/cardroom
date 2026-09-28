package com.whitejack.engine;

import com.whitejack.contract.GameDefinition;
import com.whitejack.contract.PlayerId;
import com.whitejack.contract.RoomCode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Every live room in the JVM. A room code <em>is</em> the matchmaking — there is no matchmaking
 * service, and adding one would be a second way to find a room that can disagree with the first.
 */
public final class RoomRegistry implements AutoCloseable {

    /**
     * Crockford-style base32: no I, O, 1 or 0, so a code stays unambiguous read aloud over a
     * call. Exactly 32 symbols, so six characters is 2^30 codes.
     */
    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;

    private final Map<RoomCode, RoomActor> rooms = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private final Function<String, GameDefinition<?, ?, ?>> games;
    private final Broadcaster broadcaster;
    private final TurnClock turnClock;
    private final Clock clock;

    /**
     * @param games resolves a game id to its definition. the server passes {@code catalog::require};
     *     the engine never names a game itself.
     */
    public RoomRegistry(Function<String, GameDefinition<?, ?, ?>> games, Broadcaster broadcaster,
            TurnClock turnClock, Clock clock) {
        this.games = games;
        this.broadcaster = broadcaster;
        this.turnClock = turnClock;
        this.clock = clock;
    }

    public RoomActor create(String gameId, Map<String, String> options) {
        GameDefinition<?, ?, ?> def = games.apply(gameId);
        if (def == null) throw new IllegalArgumentException("no such game: " + gameId);

        while (true) {
            RoomCode code = freshCode();
            if (rooms.containsKey(code)) continue;

            // hostId is null until someone joins: the first player through the door takes the
            // role, so a room created by an HTTP request nobody follows up on has no phantom host.
            RoomActor actor = RoomActor.start(
                    new Room(code, gameId, (PlayerId) null, options), def, broadcaster, turnClock, clock);
            if (rooms.putIfAbsent(code, actor) == null) return actor;
            actor.close(); // lost a one-in-a-billion race; drop this one rather than reuse the code
        }
    }

    public Optional<RoomActor> find(RoomCode code) {
        return Optional.ofNullable(rooms.get(code));
    }

    public int size() {
        return rooms.size();
    }

    /**
     * Closes rooms that have died or gone quiet. Nothing calls this on a timer yet — wiring it
     * to a scheduled task is the server's job, and a registry that reaps on its own would be
     * untestable without sleeping.
     *
     * @return how many rooms were reaped
     */
    public int reapIdle(Duration grace) {
        long deadline = clock.millis() - grace.toMillis();
        int reaped = 0;
        for (Map.Entry<RoomCode, RoomActor> entry : rooms.entrySet()) {
            RoomActor actor = entry.getValue();
            if (!actor.isOpen() || actor.lastActivityAt() <= deadline) {
                actor.close();
                rooms.remove(entry.getKey(), actor);
                reaped++;
            }
        }
        return reaped;
    }

    private RoomCode freshCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return new RoomCode(code.toString());
    }

    @Override
    public void close() {
        rooms.values().forEach(RoomActor::close);
        rooms.clear();
    }
}
