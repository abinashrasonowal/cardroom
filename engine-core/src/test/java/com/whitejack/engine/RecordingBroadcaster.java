package com.whitejack.engine;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.whitejack.contract.PlayerId;
import com.whitejack.contract.PlayerView;
import com.whitejack.contract.RoomCode;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The {@code :server} half of the engine, reduced to two queues. Everything the actor is
 * supposed to say ends up here, which is how a thread-based actor stays deterministic to test:
 * assert on the messages, never on a sleep.
 */
final class RecordingBroadcaster implements Broadcaster {

    record Direct(PlayerId to, Object message) {}

    private final BlockingQueue<Map<PlayerId, PlayerView>> broadcasts = new LinkedBlockingQueue<>();
    private final BlockingQueue<Direct> direct = new LinkedBlockingQueue<>();

    /** Held shut to wedge the actor mid-command, so the bounded inbox can actually fill up. */
    private final CountDownLatch gate;

    RecordingBroadcaster() {
        this(null);
    }

    RecordingBroadcaster(CountDownLatch gate) {
        this.gate = gate;
    }

    @Override
    public void toRoom(RoomCode code, Map<PlayerId, PlayerView> views) {
        await();
        broadcasts.add(views);
    }

    @Override
    public void toPlayer(PlayerId player, Object message) {
        await();
        direct.add(new Direct(player, message));
    }

    private void await() {
        if (gate == null) return;
        try {
            gate.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    Map<PlayerId, PlayerView> nextBroadcast() throws InterruptedException {
        Map<PlayerId, PlayerView> views = broadcasts.poll(2, TimeUnit.SECONDS);
        assertNotNull(views, "expected a broadcast to the room");
        return views;
    }

    Direct nextDirect() throws InterruptedException {
        Direct message = direct.poll(2, TimeUnit.SECONDS);
        assertNotNull(message, "expected a message to one player");
        return message;
    }

    /** Drains what has arrived and asserts nothing new shows up — used to prove a no-op. */
    void assertSilent() throws InterruptedException {
        assertNull(broadcasts.poll(200, TimeUnit.MILLISECONDS), "the room was told about a no-op");
    }

    void drain() {
        broadcasts.clear();
        direct.clear();
    }

    int broadcastCount() {
        return broadcasts.size();
    }
}
