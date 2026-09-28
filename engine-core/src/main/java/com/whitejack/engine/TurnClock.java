package com.whitejack.engine;

import com.whitejack.contract.RoomCode;
import com.whitejack.contract.Turn;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * One shared scheduler for every room in the JVM, not a thread per room.
 *
 * <p>It does exactly one thing on expiry — a non-blocking {@code offer} of a {@link
 * Command.Timeout} — and must never touch game state or a {@code GameDefinition}. That is what
 * keeps a couple of scheduler threads sufficient for thousands of rooms.
 *
 * <p><b>{@link #cancel} cannot recall a timeout already sitting in a room's inbox.</b> The timer
 * fires at T−1ms, the player's real intent arrives and is processed, and the queued timeout is
 * now aimed at the wrong turn. That is what {@code armedAtSeq} is for; cancelling is an
 * optimisation, the fencing token is the correctness.
 */
public final class TurnClock implements AutoCloseable {

    private final ScheduledExecutorService scheduler;
    private final Map<RoomCode, ScheduledFuture<?>> armed = new ConcurrentHashMap<>();

    public TurnClock() {
        this(Executors.newScheduledThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "turn-clock");
            thread.setDaemon(true);
            return thread;
        }));
    }

    public TurnClock(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    public void arm(RoomCode code, Turn turn, long armedAtSeq, Consumer<Command> inbox) {
        cancel(code);
        armed.put(code, scheduler.schedule(
                () -> inbox.accept(new Command.Timeout(armedAtSeq, turn.actor())),
                turn.limit().toMillis(),
                TimeUnit.MILLISECONDS));
    }

    public void cancel(RoomCode code) {
        ScheduledFuture<?> future = armed.remove(code);
        if (future != null) future.cancel(false);
    }

    @Override
    public void close() {
        armed.clear();
        scheduler.shutdownNow();
    }
}
