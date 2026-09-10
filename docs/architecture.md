# Card-Game Platform Architecture

## Goal

One platform owns reusable multiplayer and card-game concerns; each game is an installable module. Adding a game only requires a module and catalog registration, never a core-engine change.

## High-level architecture

```text
Web/mobile client
  Main UI shell: authentication, lobby, navigation, profile, wallet
    Game host route: dynamically loads a selected game's page

API / realtime gateway
  Game service: creates tables, validates requests, broadcasts events
    Core game engine: authoritative state, turns, timers, RNG, replay
      Game modules: Poker, Rummy, Blackjack, future games

Shared platform services: identity, matchmaking, payments, notifications,
analytics, database/cache, and observability.
```

Clients submit player intents such as `bet`, `draw`, and `playCard`; they never decide whether an action is legal. The server runs each game's rules through the authoritative core engine and broadcasts events.

## Suggested folder structure

```text
card-platform/
├── apps/
│   ├── web/                         # Main UI shell and game-page routes
│   │   └── src/
│   │       ├── app/                 # lobby, account, game/[gameId]/[tableId]
│   │       ├── shell/               # layout, navigation, session provider
│   │       └── game-host/           # dynamically loads game UI packages
│   ├── api/                         # HTTP API and authentication boundary
│   └── realtime-gateway/            # WebSocket connections and event fan-out
├── packages/
│   ├── core-engine/                 # generic state-machine/game runtime
│   │   └── src/
│   │       ├── contracts/           # GameDefinition, state, intents, events
│   │       ├── runtime/             # command processing, turns, timers
│   │       ├── cards/               # decks, shuffle/deal, RNG abstraction
│   │       └── replay/              # snapshots and event history
│   ├── platform-services/           # identity, matchmaking, wallet adapters
│   ├── shared-types/                # cross-package API/event types
│   ├── shared-ui/                   # card renderer, theme, reusable controls
│   └── game-sdk/                    # helpers and new-game template
├── games/
│   ├── poker/
│   │   ├── server/                  # GameDefinition and Poker rules
│   │   ├── ui/                      # Poker page mounted in the game host
│   │   └── manifest.ts              # id, metadata, server and UI entrypoints
│   ├── rummy/                       # same module layout
│   ├── blackjack/                   # same module layout
│   └── _template/                   # copy to introduce a new game
├── services/
│   ├── game-service/                # table lifecycle; hosts engine sessions
│   ├── matchmaking/
│   ├── wallet/
│   └── analytics/
├── infrastructure/                  # database, cache, queues, deployment
├── docs/
│   ├── architecture.md
│   └── card-platform-architecture.excalidraw
└── tests/
    ├── contract/                    # verifies every GameDefinition
    ├── integration/
    └── e2e/
```

## Game module contract

```ts
export interface GameDefinition<State, Intent, Event> {
  id: string;
  createInitialState(input: CreateTableInput): State;
  validateIntent(state: State, intent: Intent, actorId: string): ValidationResult;
  reduce(state: State, intent: Intent, context: EngineContext): Event[];
  isComplete(state: State): boolean;
}

export interface GameManifest {
  id: string;
  title: string;
  definition: GameDefinition<unknown, unknown, unknown>;
  loadPage: () => Promise<{ default: GamePage }>;
}
```

## Adding a new game

1. Copy `games/_template` to `games/<game-id>`.
2. Implement the server `GameDefinition` and its rule tests.
3. Build a game-specific page using `shared-ui`; it renders state and emits intents to the host.
4. Export the manifest and register it in the game catalog (or use manifest discovery).
5. Run contract, simulation, and end-to-end tests. The core engine should remain unchanged.

## Boundaries

| Boundary | Responsibility |
| --- | --- |
| Main UI shell | platform navigation/common pages; hosts game routes but contains no game rules |
| Game UI | its board, controls, animations, and state-to-view mapping |
| Core engine | generic authoritative command/event processing; independent of named games |
| Game module | game rules, legal moves, scoring, completion criteria, and page UI |
| Platform services | cross-cutting capabilities behind interfaces; no direct game-to-infrastructure coupling |
