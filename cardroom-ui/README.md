# cardroom-ui

The React client. Lobby, per-game tables, and the socket layer in `src/network/`. It holds no
rule code on the intent path: every live table renders views the server projected for you.

## Run

The production path is one jar: `./gradlew :cardroom-server:bootRun` from the repo root
builds this app (`npm run build`), bundles `dist/` as the server's static files, and serves
everything on http://localhost:8080.

For UI work with hot reload, run both:

```sh
./gradlew :cardroom-server:bootRun -PskipUi   # API + WebSocket on :8080
npm run dev                                   # Vite on :3000, proxies /api and /ws to :8080
```

Set `CARDROOM_SERVER` to point the dev proxy at a different server.

## Testing with several players

One cookie is one identity across tabs (architecture §4), so three tabs are one player.
Use separate browser profiles or a private window per player.

## What is live

High Card is server-backed. Hearts, Spades, Euchre, Oh Hell and the sandbox are offline
demos that still run their rules in the browser.
