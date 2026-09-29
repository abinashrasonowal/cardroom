# whitejack
A Multiplayer Card Game Platform

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
| `WHITEJACK_BOTS_MODEL` | OpenRouter model the bots consult (default `typesafe/jev-router`, TypeSafe's Jev). |
| `WHITEJACK_BOTS_ENABLED` | `false` turns off `POST /api/rooms/{room}/bots` (default `true`). |
| `WHITEJACK_PORT` | Compose only: host port to publish (default `8080`). |

Rooms are held in memory, so run **one** container: a restart ends games in progress, and a
second replica would not see the first one's rooms.

## Bots

The host can press **Add Jev bot** in a waiting room (or `POST /api/rooms/{room}/bots`) to fill a seat.
A bot is an ordinary client: it gets a signed token and plays over `/ws` like a browser tab. On each
turn it lists the legal moves the server's view already exposes, asks [Jev](https://openrouter.ai/~typesafe/jev-latest)
to pick one by number, and sends that move. If the reply is late (`WHITEJACK_BOTS_TIMEOUT`, default
`8s`), unparsable or out of range, it plays a built-in heuristic move instead. So a bot never sends a
move the server has not already listed as legal.

## Run locally

```sh
./gradlew :whitejack-server:bootRun                 # builds the UI too; http://localhost:8080
./gradlew :whitejack-server:bootRun -PskipUi        # API only, alongside `npm run dev` in whitejack-ui
./gradlew build                                    # everything, with tests
```
