package com.whitejack.server.service;

import com.whitejack.bots.Advisor;
import com.whitejack.bots.Bot;
import com.whitejack.bots.Brains;
import com.whitejack.contract.PlayerId;
import com.whitejack.contract.RoomCode;
import com.whitejack.engine.RoomActor;
import com.whitejack.server.dto.AddBotResponse;
import com.whitejack.server.exception.InvalidRequestException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Seats bots at a table (§14). A bot runs in this JVM but is an ordinary client: it gets a
 * freshly minted token, exactly what {@code /api/me} would hand a new browser, and dials back
 * into this server's own {@code /ws}. Nothing here touches a room directly.
 *
 * <p>No host check: anyone holding a room code may already sit down in its lobby, so adding a
 * bot grants nothing a friend with the link could not do. Seat limits and phase stay the
 * engine's call — the join is simply rejected.
 */
@Service
public class BotService implements DisposableBean {

    /** Marks a bot at the table for the UI. Cosmetic: nothing trusts it. */
    public static final String NICK_SUFFIX = " (bot)";

    private static final Duration JOIN_WAIT = Duration.ofSeconds(5);

    private final RoomService rooms;
    private final PlayerIdentityService identities;
    private final Advisor advisor;
    private final BotReasoningStore notes;
    private final boolean enabled;
    private final int maxPerRoom;
    private final Duration pace;
    private final Map<RoomCode, List<Bot>> bots = new ConcurrentHashMap<>();
    private final AtomicInteger threads = new AtomicInteger();
    private final ExecutorService executor = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "bot-" + threads.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });
    private volatile int port;

    public BotService(RoomService rooms, PlayerIdentityService identities, Advisor advisor, BotReasoningStore notes,
            @Value("${whitejack.bots.enabled:true}") boolean enabled,
            @Value("${whitejack.bots.max-per-room:7}") int maxPerRoom,
            @Value("${whitejack.bots.pace:700ms}") Duration pace) {
        this.rooms = rooms;
        this.identities = identities;
        this.advisor = advisor;
        this.notes = notes;
        this.enabled = enabled;
        this.maxPerRoom = maxPerRoom;
        this.pace = pace;
    }

    @EventListener
    public void onServerStarted(WebServerInitializedEvent event) {
        port = event.getWebServer().getPort();
    }

    public AddBotResponse add(RoomCode code) {
        if (!enabled || port <= 0) {
            throw new InvalidRequestException(HttpStatus.SERVICE_UNAVAILABLE, "BOTS_DISABLED", "bots are turned off on this server");
        }
        RoomActor room = rooms.find(code).filter(RoomActor::isOpen)
                .orElseThrow(() -> new InvalidRequestException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "no room " + code));
        if (Brains.forGame(room.gameId()).isEmpty()) {
            throw new InvalidRequestException(HttpStatus.CONFLICT, "UNSUPPORTED_GAME", "bots cannot play " + room.gameId() + " yet");
        }
        List<Bot> seated = bots.computeIfAbsent(code, c -> new CopyOnWriteArrayList<>());
        if (seated.size() >= maxPerRoom) {
            throw new InvalidRequestException(HttpStatus.CONFLICT, "TOO_MANY_BOTS", "this room already has " + seated.size() + " bots");
        }

        String token = identities.mint();
        PlayerId player = identities.verify(token).orElseThrow();
        String nick = "Jev " + (seated.size() + 1) + NICK_SUFFIX;
        Bot.Config config = new Bot.Config(URI.create("ws://127.0.0.1:" + port + "/ws"), token, player.value(),
                code.value(), nick, advisor, pace, executor, notes);
        Bot bot;
        try {
            bot = Bot.join(config).get(JOIN_WAIT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Bot.JoinRejected rejected) {
                throw new InvalidRequestException(HttpStatus.CONFLICT, rejected.error(), rejected.getMessage());
            }
            throw new InvalidRequestException(HttpStatus.SERVICE_UNAVAILABLE, "BOT_UNAVAILABLE", "the bot could not connect");
        } catch (TimeoutException e) {
            throw new InvalidRequestException(HttpStatus.SERVICE_UNAVAILABLE, "BOT_UNAVAILABLE", "the bot did not get a seat in time");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InvalidRequestException(HttpStatus.SERVICE_UNAVAILABLE, "BOT_UNAVAILABLE", "interrupted");
        }
        seated.add(bot);
        bot.finished().thenRun(() -> seated.remove(bot));
        return new AddBotResponse(player.value(), nick);
    }

    /** What this room's bots decided in its finished hands, newest first. */
    public List<BotReasoningStore.HandNotes> notes(RoomCode code) {
        return notes.finished(code.value());
    }

    public int count(RoomCode code) {
        return bots.getOrDefault(code, List.of()).size();
    }

    /** Rooms are reaped without telling anyone; their bots go with them. */
    @Scheduled(fixedDelay = 60_000)
    public void sweep() {
        bots.forEach((code, seated) -> {
            if (rooms.find(code).filter(RoomActor::isOpen).isEmpty()) seated.forEach(Bot::close);
            if (seated.isEmpty() && bots.remove(code, seated)) notes.forget(code.value());
        });
    }

    @Override
    public void destroy() {
        bots.values().forEach(seated -> seated.forEach(Bot::close));
        executor.shutdownNow();
    }
}
