# cardroom
A Multiplayer Card Game Platform

## Run with Docker

```sh
docker compose up --build          # → http://localhost:8080
```

or without Compose:

```sh
docker build -t cardroom .
docker run -p 8080:8080 -e CARDROOM_COOKIESECRET=$(openssl rand -hex 32) cardroom
```

One image serves the React UI, the REST API and the `/ws` WebSocket from a single JVM.

| Variable | Purpose |
| --- | --- |
| `CARDROOM_COOKIESECRET` | Signs the player identity cookie. Set it, or every restart gives players new identities and a warning is logged. With Compose, export `CARDROOM_COOKIE_SECRET` instead. |
| `CARDROOM_ALLOWEDORIGINS` | Extra origins allowed to open the WebSocket (same-origin always works). Default `http://localhost:*`. |
| `CARDROOM_PORT` | Compose only: host port to publish (default `8080`). |

Rooms are held in memory, so run **one** container: a restart ends games in progress, and a
second replica would not see the first one's rooms.

## Run locally

```sh
./gradlew :cardroom-server:bootRun                 # builds the UI too; http://localhost:8080
./gradlew :cardroom-server:bootRun -PskipUi        # API only, alongside `npm run dev` in cardroom-ui
./gradlew build                                    # everything, with tests
```
