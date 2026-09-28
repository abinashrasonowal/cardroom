import React from 'react';
import { LobbyView } from '@/types/wire';
import { soundFx } from '@/utils/audio';

interface WaitingRoomProps {
  lobby: LobbyView | null;
  playerId: string;
  /** How many players the game needs before the host may start. */
  minPlayers: number;
  onStart: () => void;
}

/** Before the host starts: who is here, and the Start button. Same for every live game. */
export const WaitingRoom: React.FC<WaitingRoomProps> = ({ lobby, playerId, minPlayers, onStart }) => {
  const members = lobby?.members ?? [];
  const isHost = lobby?.host === playerId;
  const ready = members.length >= minPlayers;

  return (
    <section className="border border-slate-200 rounded-2xl p-6 flex flex-col gap-4">
      <div className="flex items-baseline justify-between">
        <h2 className="text-xl font-bold font-space text-slate-950">Waiting room</h2>
        <span className="text-xs font-semibold text-slate-500">
          {members.length}/{minPlayers} players
        </span>
      </div>
      <ul className="flex flex-col divide-y divide-slate-100">
        {members.map((m) => (
          <li key={m.id} className="flex items-center justify-between py-2.5">
            <span className="flex items-center gap-2.5 text-sm text-slate-900">
              <span className={`w-2 h-2 rounded-full ${m.connected ? 'bg-emerald-500' : 'bg-slate-300'}`} />
              {m.nick}
              {m.id === playerId && <span className="text-xs text-slate-500">(you)</span>}
            </span>
            {m.id === lobby?.host && (
              <span className="text-xs font-bold uppercase tracking-wider text-blue-600">Host</span>
            )}
          </li>
        ))}
      </ul>
      {isHost ? (
        <button
          type="button"
          onClick={() => {
            soundFx.playCardDeal();
            onStart();
          }}
          disabled={!ready}
          className="self-start h-11 px-6 rounded-lg bg-blue-600 text-white text-xs font-bold uppercase tracking-wider hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
        >
          {ready ? 'Start game' : `Need ${minPlayers} players (${members.length}/${minPlayers})`}
        </button>
      ) : (
        <p className="text-sm text-slate-600">Waiting for the host to start…</p>
      )}
    </section>
  );
};
