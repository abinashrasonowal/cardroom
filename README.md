# whitejack
A Multiplayer Card Game Platform

## Games

These run live on the server, with the server enforcing every rule:

| Game | Players | Summary |
| --- | --- | --- |
| **Poker** | 2–8 | No-limit Texas Hold'em freezeout: 1,000 chips each, blinds 10/20 doubling every 10 hands, side pots for all-ins. Last player with chips wins. |
| **Hearts** | 4 | Pass three cards, then duck every heart and the Q♠, or take all 26 points to shoot the moon. First to 100 ends it; lowest score wins. |
| **Gin Rummy** | 2 | Build sets and runs, then knock at 10 deadwood or go gin; watch for the undercut. First to 100 wins. |

## Run with Docker

```sh
docker compose up --build          # → http://localhost:8080
```

or without Compose:

```sh
docker build -t whitejack .
docker run -p 8080:8080 -e WHITEJACK_COOKIESECRET=$(openssl rand -hex 32) whitejack
```

One image serves the React UI, the REST API and the `/ws` WebSocket from a single JVM.

| Variable | Purpose |
| --- | --- |
| `WHITEJACK_COOKIESECRET` | Signs the player identity cookie. Set it, or every restart gives players new identities and a warning is logged. With Compose, export `WHITEJACK_COOKIE_SECRET` instead. |
| `WHITEJACK_ALLOWEDORIGINS` | Extra origins allowed to open the WebSocket (same-origin always works). Default `http://localhost:*`. |
| `WHITEJACK_BOTS_OPENROUTERAPIKEY` | OpenRouter API key for the bots' model. Unset, bots still play legal heuristic moves. With Compose, export `OPENROUTER_API_KEY` instead. |
| `WHITEJACK_BOTS_MODEL` | Decision model the bots consult (default `~typesafe/jev-latest`, TypeSafe's Jev). |
| `WHITEJACK_BOTS_ENDPOINT` | Decisions endpoint (default OpenRouter's `https://openrouter.ai/api/alpha/decisions`; TypeSafe's own `https://api.typesafe.ai/v1/systemone` with model `jev-latest` also works). |
| `WHITEJACK_BOTS_ENABLED` | `false` turns off `POST /api/rooms/{room}/bots` (default `true`). |
| `WHITEJACK_PORT` | Compose only: host port to publish (default `8080`). |

Rooms are held in memory, so run **one** container: a restart ends games in progress, and a
second replica would not see the first one's rooms.

## Bots

The host can press **Add Jev bot** in a waiting room (or `POST /api/rooms/{room}/bots`) to fill a seat.
A bot is an ordinary client: it gets a signed token and plays over `/ws` like a browser tab. On each
turn it lists the legal moves the server's view already exposes and asks
[Jev](https://openrouter.ai/~typesafe/jev-latest), TypeSafe's decision model, a typed `choice` question
whose options are exactly those moves. Jev returns a probability for each one, and the bot plays the best
(for the three-card Hearts pass, the best three). If Jev is slow (`WHITEJACK_BOTS_TIMEOUT`, default `3s`)
or unavailable, the bot plays a built-in heuristic move instead. So a bot never sends a move the server
has not already listed as legal.

## Run locally

```sh
./gradlew :whitejack-server:bootRun                 # builds the UI too; http://localhost:8080
./gradlew :whitejack-server:bootRun -PskipUi        # API only, alongside `npm run dev` in whitejack-ui
./gradlew build                                    # everything, with tests
```
