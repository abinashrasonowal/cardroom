# Cardroom — Architecture

A room-based multiplayer card platform. One person creates a room, shares the code,
everyone else joins and plays. No sign-up, no profile, no wallet.

**Implementation: Java 17.** Four Gradle subprojects, one deployable JVM.

Java 17, not 21, because the dev box already runs 17 for another project and nothing here
needs Loom: rooms are counted in hundreds, so a platform thread each is affordable. The
two costs are named in §10. Moving to 21 is one line in the Gradle toolchain block plus
the thread construction in `RoomActor`.

The server is authoritative for every rule. The room creator ("host"/"master") holds a
**role**, not authority: their privileges are lobby-level only.

---

## 0. Build order

The architecture is one process, but the *work* still needs sequencing. Milestones 1–4
are the project; 5–6 are optional polish.

| # | Milestone | Proves |
| - | --------- | ------ |
| 1 | Gateway + room actor + **High Card** (deal one card each, highest wins, ~80 lines) end to end | The loop works |
| 2 | Reconnect + the projection property test | The two claims that matter |
| 3 | **Blackjack**, without touching `engine-core` — provable on the git history | The plug-in story |
| 4 | Bots + landing page that spectates a live bot table | It demos to a stranger |
| 5 | Commit-reveal + `/verify` page | Trust story |
| 6 | Rummy | Second real game |

Start with High Card, not Blackjack. It shakes out the contract without rules noise, and
the commit where Blackjack lands with `engine-core` untouched *is* the portfolio artifact.

---

## 1. Goals and non-goals

**Goals**

| # | Goal | Why it matters |
| - | ---- | -------------- |
| G1 | Server-authoritative rules | A client can never fabricate a win. |
| G2 | Games are plug-ins, discovered at runtime | Adding a game = drop a jar. Core never recompiles. |
| G3 | Survivable sessions | Refresh or network blip must not cost the hand. |
| G4 | Deal fixed before play, verifiable after | Not "provably fair" — see §9 for what is actually proved. |
| G5 | Instantly demoable | A stranger opens the URL alone and sees a live game in one click. |

**Non-goals (deliberate)**

No accounts, passwords, email, OAuth, profiles. No wallet, currency, or payments. No
matchmaking service — a room code *is* the matchmaking. No microservices, no broker, no
Kubernetes. No client-side prediction: turns are human-paced, so prediction buys nothing
and costs a rollback system.

---

## 2. Component map

```text
CLIENTS (browser tabs) ── host · players · spectators · bots
   │  intents (WS)              ▲  projected views (WS)
   ▼                            │
GAME SERVER — one JVM
   1 WS GATEWAY      cookie→PlayerId · rate limit · envelope parse · JSON
   2 ROOM REGISTRY   roomCode → RoomActor · create/join/GC
     └ ROOM ACTOR    one thread + bounded queue per room (single writer)
   3 ENGINE RUNTIME  validate → reduce → fold → append → advance
                     turn clock · seeded RNG · timeout auto-play
   4 PROJECTION      one PlayerView per viewer; hidden cards never leave the JVM
   5 BROADCAST       fan out via the Broadcaster port
   │  calls rules                ▲  Event[]
   ▼                            │
GAME MODULES (pure)  high-card · blackjack · rummy
```

State lives in memory: `roomCode → RoomActor`, plus a per-hand event log.
Persistence is deferred (§14) but the log is designed to be persistable from day one.

---

## 3. Dependency rules

```text
client → server → engine-core → engine-contract ← games-*
```

`engine-contract` is a leaf. Both `engine-core` and every game compile against it, and
that is the entire reason a game can be written without the engine on its classpath.

1. A **game module** depends on `engine-contract` only — which is why the card vocabulary
   (`Card`, `Deck`, `Shuffler`, `RandomSource`) lives there, not in `engine-core`. A game
   module may not touch sockets, JDBC, or Spring.
2. **engine-core** depends on `engine-contract` and never on a named game. Games arrive
   through `ServiceLoader`. Enforced in CI:
   ```java
   noClasses().that().resideInAPackage("..engine..")
       .should().dependOnClassesThat().resideInAPackage("..games..")
   ```
3. The **room actor** owns mutation. Nothing else writes room state.
4. The **gateway** knows sockets and `PlayerId`. It knows no card rules. It implements
   the ports `engine-core` declares (`Broadcaster`), never the reverse.
5. The **client** ships no rule code *on the intent path*. One deliberate exception: the
   `/verify` page (§9) runs the game module's pure `apply` fold over an already-public
   log. That is an audit tool, not gameplay, and it must run client-side or it proves
   nothing about the server.

**Classpath, not JPMS.** Discovery uses `META-INF/services`, which the module path
ignores in favour of `provides…with`. Pick one; classpath plus ArchUnit gives the same
guarantee for none of the `module-info` ceremony. "Module" below always means
"Gradle subproject".

---

## 4. Identity without accounts

```text
first HTTP request → httpOnly signed cookie { playerId }   (no password, no user row)
WS connect         → cookie verified → PlayerId
join               → { roomCode, nickname, clientSeed }
refresh / blip     → same cookie → same seat, hand restored
```

- The cookie *is* the session. Nickname lives in `localStorage`; it is a label.
- **Cookie fallback.** The same token is mirrored into `localStorage` and may be
  presented in the WS connect frame when the cookie is absent (Safari ITP, embedded
  webviews, cookies blocked). G5 dies without this.
- **One cookie = one identity across tabs.** Consequences: `Seat.connected` must be a
  *set of sockets*, not a boolean — closing one tab must not fire the turn timer. One
  person cannot occupy two seats, which is a useful anti-collusion property. And
  "open three tabs" is **not** a working local-dev story: use three browser profiles, or
  a dev-only `?as=` override.

---

## 5. Core model

```java
record PlayerId(String value) {}
record RoomCode(String value) {}                 // Crockford base32, 6 chars ≈ 2^30
record Seat(int index, PlayerId id, String nick, Set<SocketId> sockets) {}

final class Room {
    RoomCode code; String gameId; PlayerId hostId;
    List<Seat> seats; Phase phase;               // LOBBY | PLAYING | FINISHED
    Map<String,String> options;                  // variant config, opaque to the engine
    List<Hand> hands;
}

record Hand(int index, String commit, byte[] serverSeed, long startSeq, long endSeq) {}

record SequencedEvent<E extends GameEvent>(long seq, long at, int v, E event) {}
```

**`Hand`, not `Room`, owns the seed.** A room plays many hands; reusing one seed means
that after the first reveal every player can compute the next deck. New seed, new commit,
every hand.

**Room codes.** Crockford base32 excludes `I O 1 0`, so codes are unambiguous when read
aloud. Regenerate on collision (loop until the registry is free). Rate-limit *join*
attempts per IP hard, and let the host lock the room at `PLAYING` — Jackbox shipped
4-character codes and got public room scanners for its trouble.

---

## 6. Two kinds of message

This is the distinction the flow turns on: **room commands** are the engine's business,
**game intents** are the module's. A blackjack module has no opinion about kicking a
player.

```java
sealed interface Command
    permits Join, Leave, Disconnect, Reconnect, Submit,
            Timeout, TransferHost, Kick, Start, Close {}

record Submit(PlayerId actor, JsonNode intent, String clientMsgId) implements Command {}
record Timeout(long armedAtSeq, PlayerId actor) implements Command {}
```

Only `Submit` reaches the game module. Everything else the room actor handles itself,
which is what keeps `Phase` transitions and host powers out of game code.

**Seating rules belong to the game**, though: `canJoin(state, seat)` on the contract
answers "may someone sit down mid-hand?" so that blackjack's answer never gets hardcoded
into the engine.

---

## 7. Wire protocol

Every frame is JSON with the same envelope. Without this there is nothing to code against.

```jsonc
// client → server
{ "v": 1, "id": "c17", "type": "intent", "payload": { "type": "hit" } }
{ "v": 1, "id": "c18", "type": "join",   "payload": { "room": "K7M2QX", "nick": "abi",
                                                      "clientSeed": "9f3a…" } }

// server → client
{ "v": 1, "re": "c17", "type": "accepted", "seq": 412 }
{ "v": 1, "re": "c17", "type": "rejected", "error": "NOT_YOUR_TURN", "detail": "…" }
{ "v": 1, "type": "sync",   "seq": 412, "view": { … } }   // full view: join + reconnect
{ "v": 1, "type": "update", "seq": 413, "view": { … } }   // every subsequent change
```

- `id` / `re` correlate a reply with the intent that caused it. Without it a client with
  two intents in flight cannot tell which was rejected.
- `error` is a code from a closed enum, not prose. `detail` is for humans.
- `v` is the protocol version. Heartbeat: ping every 15s, drop at 45s.

**Views, not events, go over the wire.** The engine broadcasts a freshly projected
`PlayerView` per viewer on every change. The event log stays inside the JVM.

This is the single most important consequence of §8: an event like
`Dealt(seat=2, cards=[A♠,K♥])` carries hidden information by construction, so a design
that ships the event stream needs a *second* filter (boardgame.io needs both
`playerView` and `redactLog` for exactly this). One filter, applied on one code path, is
both simpler and the only version I can be confident has no leak. At ≤8 seats the
bandwidth argument for deltas is nil.

Animations still work: `project` may include a `lastActions` field describing what just
changed *as this viewer is allowed to see it*. It goes through the same filter.

---

## 8. The game module contract

```java
public interface GameDefinition<S, I extends Intent, E extends GameEvent> {

    String id();
    int    version();                     // stamped on every event; replay needs it
    int    minPlayers();
    int    maxPlayers();

    // The gateway parses JSON and cannot produce an I. Only the definition can.
    I parseIntent(JsonNode raw);

    S          createInitialState(List<Seat> seats, Map<String,String> options, EngineContext ctx);
    Validation canJoin(S state, Seat seat);
    Validation validate(S state, I intent, PlayerId actor);
    List<E>    reduce(S state, I intent, EngineContext ctx);   // pure
    S          apply(S state, E event);                        // pure fold

    Optional<Turn> turn(S state);         // who is on the clock, and for how long
    I              onTimeout(S state, PlayerId actor);

    PlayerView project(S state, Optional<PlayerId> viewer);    // empty = spectator

    boolean isHandComplete(S state);
    boolean isComplete(S state);
}

record Turn(PlayerId actor, Duration limit) {}
sealed interface Validation permits Ok, Reject {}
public interface PlayerView {}            // marker: "this must serialize to JSON"
```

Four of these exist only because the alternative is editing the core when game #2 arrives:

- **`parseIntent`** — `GameCatalog.require(id)` returns `GameDefinition<?,?,?>`, and
  nothing but `null` is assignable to a captured `I`. Without this method the generics
  collapse into raw types and unchecked casts at every call site. `GameSession`'s entry
  point therefore takes JSON: `Outcome submit(JsonNode raw, PlayerId actor)`.
- **`turn(state)`** — the runtime must arm a clock for the next actor, and `S` is opaque
  to it. A single engine-wide `TURN_LIMIT` is wrong too: blackjack wants ~15s, rummy ~45s,
  a lobby none. `Optional.empty()` means nobody is on the clock.
- **`options`** — blackjack needs deck count and dealer-hits-soft-17. Adding the
  parameter later is a breaking change across every plug-in.
- **`isHandComplete` vs `isComplete`** — the seed reveal and the next deal need a hand
  boundary; one boolean cannot express both.

**`project` is the only redaction primitive in the system.** If hidden state reaches a
client by any other path, the server-authoritative claim is decorative.

**Contract obligation:** `onTimeout` must return an intent that `validate` accepts for
that state. Otherwise: reject → no state change → clock re-arms → same timeout → the room
freezes alive. The engine defends this anyway (§11), and a contract test asserts it.

---

## 9. One intent, end to end

```text
0  gateway.onMessage(socket, json)
     PlayerId actor = cookie.verify(socket);
     if (!registry.find(code).offer(new Submit(actor, payload, msgId)))
         socket.close(OVERLOADED);                       // bounded queue = backpressure
   ─────────── everything below runs on that room's single thread ───────────
1  I intent = def.parseIntent(raw);
   Validation v = def.validate(state, intent, actor);
     if (v instanceof Reject r) { broadcaster.toPlayer(actor, rejected(msgId, r)); return; }
2  List<E> events = def.reduce(state, intent, ctx);       // pure; time + rng via ctx
3  S next = events.stream().reduce(state, def::apply);    // fold FIRST
   long at = ctx.now();
   for (E e : events) seq = log.append(e, at, def.version());
   this.state = next;                                     // swap only on success
4  broadcaster.toRoom(code, viewsFor(next));              // tell them, THEN start the clock
5  def.turn(next).ifPresentOrElse(
       t -> turnClock.arm(code, t, seq),                  // seq = fencing token
       () -> turnClock.cancel(code));
6  if (def.isHandComplete(next)) reveal(hand.serverSeed());
```

**Rejection is a total no-op:** no event, no `seq` bump, no broadcast to anyone but the
caller, and no other player learns the attempt was made.

**Fold before append.** If `apply` throws on event 3 of 5, appending first leaves a log
the state does not reflect and replay diverges permanently. Compute into a local, swap at
the end.

**Broadcast before arming.** Otherwise the player's clock starts before they are told it
is their turn, and network latency is deducted from their thinking time. The view also
carries an absolute `deadlineAt` plus a connect-time offset estimate, so client countdowns
don't drift.

---

## 10. Concurrency

One `RoomActor` = one thread = one writer.

```java
Thread t = new Thread(this, "room-" + code);
t.setDaemon(true);            // a live room must never hold the JVM open
t.start();
```

On 21 this becomes `Thread.ofVirtual().name("room-" + code).start(this)` and nothing else
changes: the actor blocks on `inbox.take()` and does no other blocking I/O, so carrier
pinning is not a concern either way.

```java
private final BlockingQueue<Command> inbox = new ArrayBlockingQueue<>(256);
boolean offer(Command c) { return inbox.offer(c); }   // false → gateway closes the socket
```

**Bounded, and `offer` returns a signal.** The room thread is the slow side of this pipe
(it runs `reduce` plus N projections per command), so an unbounded queue turns one
looping client into an OOM. LMAX's ring buffer is bounded by construction for the same
reason: overload should become a rejection, never heap exhaustion.

**Stale-timer fencing.** `Timeout` carries the `seq` it was armed at; the room drops any
timeout whose `armedAtSeq != seq`. Without it: the timer fires at T−1ms and queues a
`Timeout`; the player's real intent arrives and is processed; `cancel()` cannot recall a
message already in the queue; the room then auto-plays the *next* player's turn instantly.
At 15-second turns and human reaction times that is a weekly occurrence, not an edge case.

`TurnClock` uses one shared `ScheduledExecutorService` for the whole JVM, not a thread per
room. It only does a non-blocking `inbox.offer` — it must never touch `def.*` or `state`,
which is what keeps a handful of scheduler threads sufficient for every room at once.

**Safe publication.** `private volatile S state`. Records are shallowly immutable: a
`record BjState(List<Card> deck)` holding an `ArrayList` is mutable behind a façade, so
compact constructors do `deck = List.copyOf(deck)`.

**A game module that throws must not silently brick the room.** An uncaught exception out
of `reduce` exits the actor loop, kills the thread, and leaves the registry handing out a
room whose queue nobody drains — no log line, no alert, sockets hanging forever.

```java
while (open) {
    Command c;
    try { c = inbox.take(); }
    catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
    try { handle(c); }
    catch (RuntimeException e) {
        log.error("room {} command {} failed", code, c, e);
        broadcaster.toRoom(code, faulted());
        open = false;                                   // fail loudly, then close
    }
}
```

**Known ceilings.** A platform thread per room costs ~1MB of stack reserved, so this
design holds thousands of live rooms per JVM, not millions. That is far past the point
where §17's sharding applies, so it is not the binding constraint.

Neither platform nor virtual threads are preemptible, so a game module looping forever
inside `reduce` wedges that room's thread permanently. Game modules are trusted first-party code; the mitigation
is a cap on `events.size()` and this paragraph, not a sandbox.

---

## 11. Disconnect, reconnect, and the host

**Reconnect** — snapshot only, no event tail:

```java
record Snapshot(long seq, PlayerView view) {}
```

The snapshot already reflects every event up to `seq`; sending a tail as well makes the
client double-apply unless it knows the snapshot's sequence number. Send `sync`, done.

| Event | Behaviour |
| ----- | --------- |
| Socket closes, others remain for that `PlayerId` | Nothing. Identity is per-cookie, not per-socket. |
| Last socket closes mid-turn | Seat held for a grace window; turn clock keeps running; on expiry `onTimeout` fires |
| Last socket closes off-turn | Seat marked disconnected, held for the grace window; play continues |
| Host drops | Role transfers to the longest-seated connected player. **The room does not close.** |
| Grace window expires | Seat released; a spectator may take it |
| All seats empty | Room GC'd after ~5 min |

`Disconnect` is a distinct `Command` from `Leave` — dropping is not quitting, and only the
former holds the seat. A rejected `onTimeout` intent is logged and force-closes the hand
rather than re-arming, so a buggy default move cannot freeze the room.

---

## 12. Randomness and what is actually proved

```text
join      each player sends a random clientSeed
hand start serverSeed = 32 secure bytes;  commit = sha256(serverSeed)  → broadcast
          handSeed = HMAC-SHA256(serverSeed, clientSeed₁‖…‖clientSeedₙ‖handIndex)
play      RandomSource = HmacRandom(handSeed), counter-based, rejection-sampled nextInt
hand end  broadcast serverSeed
verify    sha256(serverSeed) == commit  AND  replay(log, handSeed) == what was broadcast
```

**Why the client seeds.** A server that picks the seed alone can grind: generate ten
thousand candidate seeds, simulate each deal, publish the commit for the one where the
house wins. The commit and the reveal both verify perfectly. Mixing in a client
contribution means no single party can steer the deal — this is why every production
provably-fair implementation derives outcomes as `HMAC(serverSeed, clientSeed:nonce)`.
It is roughly fifteen extra lines and it is the difference between "the server didn't
change its mind" and "nobody could rig this".

**Not `SplittableRandom`.** Its only constructor takes a `long`, so 32 secure bytes become
8, and JavaScript has no equivalent — the `/verify` page would need a hand-ported
SplitMix64 that stays bit-identical across JDK upgrades. An HMAC-SHA256 counter stream
consumes the full seed and is twelve lines of WebCrypto in the browser, forever.

**The commit and the reveal are log events** (`HandStarted(commit, seats, options)` at the
hand's seq 0, `SeedRevealed(serverSeed)` at the end), not fields hanging off `Room`. They
are then ordered relative to every card event, arrive on reconnect for free, and no
verifier can be shown a different commit than the players saw.

**What this does not prove:** folding `apply` over the server's own log only shows `apply`
is deterministic — a rigged shuffle replays perfectly. The claim rests on re-deriving the
deal *from the revealed seed* and matching it against what was broadcast at the time.

---

## 13. Module layout

```text
cardroom/
├── engine-contract/        Intent · GameEvent · PlayerId · RoomCode · Seat · Turn
│                           Validation · PlayerView · EngineContext · GameDefinition
│                           RandomSource · Card · Rank · Suit · Deck · Hand · Shuffler
├── engine-core/            GameSession · EventLog<E> · TurnClock · RoomRegistry
│                           RoomActor · Room · GameCatalog · HmacRandom · SeedCommit
│                           ports: Broadcaster, EventStore      (java.time.Clock for time)
├── games/
│   ├── high-card/  blackjack/  rummy/  _template/
├── server/                 WS gateway, cookies, JSON, HTTP, main()
├── client/                 lobby · table · per-game UI (lazy) · net/reconnect
├── bots/                   headless clients speaking the public protocol
└── docs/
```

Four subprojects plus games. The `engine-contract` split is not ceremony — it is the thing
that lets a game compile without the engine on its classpath, which is the whole plug-in
claim. `server` depends on games as `runtimeOnly`: the jar reaches the runtime classpath,
the compiler never sees it.

**The card vocabulary lives in `engine-contract`**, not `engine-core`. `Card`, `Deck` and
`Shuffler` are pure functions of `RandomSource` with no infrastructure dependency, and
`EngineContext` exposes `RandomSource` — putting it in `engine-core` would both create a
compile cycle and leave games unable to reference a playing card.

Providers must be `public` with a public no-arg constructor, declared in
`META-INF/services/com.cardroom.contract.GameDefinition`.

---

## 14. Demoability

An empty room is an empty portfolio. Bots are ordinary WS clients using the public
protocol with no backdoor — they double as the load test and as proof the protocol is
complete. Default the landing page to *spectate a live bot table*, with one-click
"take a seat".

---

## 15. Testing

| Layer | Test |
| ----- | ---- |
| Game module | pure unit tests on `validate` / `reduce`; no mocks needed |
| Contract | one suite every `GameDefinition` must pass, incl. `validate(s, onTimeout(s,p), p) == Ok` |
| `projectionNeverLeaksHiddenCards()` | property test: serialize the view, assert no unseen card appears in the JSON |
| `illegalIntentIsTotalNoOp()` | state, seq and log unchanged; only the caller hears |
| `replayFromSeedMatchesBroadcast()` | re-derive the deal from the revealed seed |
| `oneWriterPerRoom()` | concurrent submits produce a total order |
| `gameDefinitionIsPure()` | ArchUnit ban list (below) |
| Integration | two socket clients play a scripted hand end to end |

Write the projection test before the first game. It is the only one that guards the
project's central claim.

**ArchUnit ban list for game modules:** `System.currentTimeMillis`, `System.nanoTime`,
`Instant.now`, `LocalDate.now`, `new Random()`, `Math.random`, `ThreadLocalRandom`, and
**`UUID.randomUUID()`** — that last is the one an author reaches for innocently to id a
card or a trick, and it silently destroys replay. Everything arrives through
`EngineContext`. Scope limit, stated honestly: ArchUnit sees classes in this repo, not a
third-party jar dropped on the classpath.

---

## 16. Engine invariants

1. Game state is mutated by exactly one thread; no lock protects it. (Not "no locks
   exist" — `ConcurrentHashMap` and `ArrayBlockingQueue` hold plenty internally.)
2. `state` is `volatile` and every `S` is deeply immutable, so `project` and `replay` are
   safe to run off the room thread.
3. A `GameDefinition` is a `ServiceLoader` singleton shared by every room in the JVM:
   implementations are **stateless and thread-safe**; all state lives in `S`. A single
   mutable field on `BlackjackGame` corrupts every room at once — the likeliest bug a
   plug-in author will write.
4. Same seed + same intent sequence ⇒ byte-identical event sequence.
5. An illegal intent is a total no-op.
6. No projected payload contains a card the viewer is not entitled to see.

Each is a test in §15, not a comment.

---

## 17. When one JVM is not enough

| Symptom | Change |
| ------- | ------ |
| More than one instance | Redis pub/sub behind the `Broadcaster` port + sticky routing by `roomCode` |
| Rooms must survive a deploy | Implement `EventStore`; rebuild by folding the log on boot |
| Hand history / stats | Archive completed hands to Postgres; live path unchanged |

The replay unit must be self-contained or none of this works:
`{gameId, gameVersion, seatsAtStart, options, commit, serverSeed, clientSeeds, log[start..end]}`.
Event seq 0 of each hand is `HandStarted(...)` carrying exactly that, so the initial state
is derivable rather than remembered. `SequencedEvent` carries `v` from day one —
retrofitting a version field onto already-persisted events is the one migration you cannot
perform.

State is already partitioned by room, so sharding is placement, not redesign.

---

## 18. Skipped on purpose

Accounts, wallet, matchmaking, notifications, analytics, microservices, message broker,
ORM, cache, JPMS, client-side prediction. Each can be added later without touching the
engine.
