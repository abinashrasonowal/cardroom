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
| `WHITEJACK_PORT` | Compose only: host port to publish (default `8080`). |

Rooms are held in memory, so run **one** container: a restart ends games in progress, and a
second replica would not see the first one's rooms.

## Run locally

```sh
./gradlew :whitejack-server:bootRun                 # builds the UI too; http://localhost:8080
./gradlew :whitejack-server:bootRun -PskipUi        # API only, alongside `npm run dev` in whitejack-ui
./gradlew build                                    # everything, with tests
```
