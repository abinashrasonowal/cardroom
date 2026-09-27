import { useEffect, useRef, useState } from 'react';
import { LobbyView, ServerFrame } from '@/types/wire';
import { CardroomSocket, SocketStatus, clientSeedFor } from './socket';

export interface RoomError {
  error: string;
  detail: string;
}

/** Errors after which staying on the table is pointless. */
const FATAL = new Set(['ROOM_NOT_FOUND', 'FAULT']);

/**
 * One live room. The latest lobby is kept after the game starts because game views carry no
 * host, and only the host may close the room.
 */
export function useRoom(room: string, nick: string, token: string) {
  const socket = useRef<CardroomSocket | null>(null);
  const [status, setStatus] = useState<SocketStatus>('connecting');
  const [lobby, setLobby] = useState<LobbyView | null>(null);
  const [game, setGame] = useState<unknown | null>(null);
  const [error, setError] = useState<RoomError | null>(null);

  useEffect(() => {
    const s = new CardroomSocket(token);
    socket.current = s;
    setLobby(null);
    setGame(null);
    setError(null);

    s.onStatus(setStatus);
    s.onFrame((frame: ServerFrame) => {
      switch (frame.type) {
        case 'update':
        case 'sync':
          if (frame.viewType === 'lobby') setLobby(frame.view);
          else setGame(frame.view);
          break;
        case 'accepted':
          setError(null);
          break;
        case 'rejected':
          setError({ error: frame.error, detail: frame.detail });
          if (FATAL.has(frame.error)) s.close();
          break;
        case 'fault':
          setError({ error: 'FAULT', detail: frame.detail });
          s.close();
          break;
      }
    });
    s.join(room, nick, clientSeedFor(room));
    s.connect();
    return () => s.close();
  }, [room, nick, token]);

  const send = (type: string, payload?: object) => socket.current?.send(type, payload) ?? '';

  return {
    status,
    lobby,
    game,
    error,
    fatal: error != null && FATAL.has(error.error),
    start: () => send('start'),
    intent: (payload: object) => send('intent', payload),
    leave: () => send('leave'),
    closeRoom: () => send('close'),
  };
}
