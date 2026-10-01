// The small HTTP surface. Everything after "create a room" happens over the socket.
import { readLocal, writeLocal } from '@/utils/storage';

export interface Me {
  playerId: string;
  token: string;
}

export interface GameInfo {
  id: string;
  minPlayers: number;
  maxPlayers: number;
}

const TOKEN_KEY = 'whitejack.token';

/** §4 fallback: the token is mirrored here for browsers that withhold the cookie. */
export function storedToken(): string | null {
  return readLocal(TOKEN_KEY);
}

function identityHeaders(): HeadersInit {
  const token = storedToken();
  return token ? { 'X-Whitejack-Token': token } : {};
}

async function asJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.detail || body?.error || `HTTP ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export async function getMe(): Promise<Me> {
  const me = await asJson<Me>(await fetch('/api/me', { headers: identityHeaders() }));
  writeLocal(TOKEN_KEY, me.token);
  return me;
}

export async function listGames(): Promise<GameInfo[]> {
  return asJson<GameInfo[]>(await fetch('/api/games', { headers: identityHeaders() }));
}

export async function createRoom(gameId: string): Promise<string> {
  const created = await asJson<{ room: string }>(
    await fetch('/api/rooms', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...identityHeaders() },
      body: JSON.stringify({ gameId }),
    })
  );
  return created.room;
}

export interface AddedBot {
  playerId: string;
  nick: string;
}

/** Seats a Jev bot in the room's lobby. It joins over the socket like anyone else. */
export async function addBot(room: string): Promise<AddedBot> {
  return asJson<AddedBot>(
    await fetch(`/api/rooms/${encodeURIComponent(room)}/bots`, {
      method: 'POST',
      headers: identityHeaders(),
    })
  );
}

/** The server marks bot nicknames this way. Display only; nothing trusts it. */
export const BOT_NICK_SUFFIX = ' (bot)';

export function isBotNick(nick: string): boolean {
  return nick.endsWith(BOT_NICK_SUFFIX);
}
