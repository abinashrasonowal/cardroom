import React, { useState } from 'react';
import { BOT_NICK_SUFFIX, isBotNick } from '@/network/api';
import { LobbyView } from '@/types/wire';
import { soundFx } from '@/utils/audio';

interface WaitingRoomProps {
  room: string;
  copied: boolean;
  onCopyInvite: () => void;
  lobby: LobbyView | null;
  playerId: string;
  /** How many players the game needs before the host may start. */
  minPlayers: number;
  /** Seats at the table; the host can add bots until it is full. */
  maxPlayers: number;
  onStart: () => void;
  onAddBot: () => Promise<unknown>;
}

/** Before the host starts: who is here, and the Start button. Same for every live game. */
export const WaitingRoom: React.FC<WaitingRoomProps> = ({
  room,
  copied,
  onCopyInvite,
  lobby,
  playerId,
  minPlayers,
  maxPlayers,
  onStart,
  onAddBot,
}) => {
  const members = lobby?.members ?? [];
  const isHost = lobby?.host === playerId;
  const ready = members.length >= minPlayers;
  const [adding, setAdding] = useState(false);
  const [botError, setBotError] = useState<string | null>(null);

  const addBot = async () => {
    soundFx.playClick();
    setAdding(true);
    setBotError(null);
    try {
      await onAddBot();
    } catch (e) {
      setBotError(e instanceof Error ? e.message : 'Could not add a bot');
    } finally {
      setAdding(false);
    }
  };

  return (
    <section className="border border-slate-200 rounded-2xl p-6 flex flex-col gap-4">
      <div className="flex items-baseline justify-between">
        <h2 className="text-xl font-bold font-space text-slate-950">Waiting room</h2>
        <span className="text-xs font-semibold text-slate-500">
          {members.length}/{minPlayers} players
        </span>
      </div>
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-violet-50 border border-violet-100 px-4 py-3">
        <div className="flex flex-col gap-0.5 min-w-0">
          <span className="text-[11px] font-bold uppercase tracking-widest text-violet-600">Invite friends</span>
          <code className="font-mono-code text-sm text-slate-800 truncate">
            {window.location.host}/#{room}
          </code>
        </div>
        <button
          type="button"
          onClick={onCopyInvite}
          className="h-9 px-4 rounded-lg bg-violet-600 text-white text-xs font-bold uppercase tracking-wider hover:bg-violet-700 cursor-pointer"
        >
          {copied ? '✓ Copied' : 'Copy link'}
        </button>
      </div>
      <ul className="flex flex-col divide-y divide-slate-100">
        {members.map((m) => (
          <li key={m.id} className="flex items-center justify-between py-2.5">
            <span className="flex items-center gap-2.5 text-sm text-slate-900">
              <span className={`w-2 h-2 rounded-full ${m.connected ? 'bg-emerald-500' : 'bg-slate-300'}`} />
              {isBotNick(m.nick) ? m.nick.slice(0, -BOT_NICK_SUFFIX.length) : m.nick}
              {isBotNick(m.nick) && (
                <span className="text-[10px] font-bold uppercase tracking-wider text-violet-700 bg-violet-50 border border-violet-200 rounded px-1.5 py-0.5">
                  Jev bot
                </span>
              )}
              {m.id === playerId && <span className="text-xs text-slate-500">(you)</span>}
            </span>
            {m.id === lobby?.host && (
              <span className="text-xs font-bold uppercase tracking-wider text-blue-600">Host</span>
            )}
          </li>
        ))}
      </ul>
      {isHost ? (
        <div className="flex flex-wrap items-center gap-3">
          <button
            type="button"
            onClick={() => {
              soundFx.playCardDeal();
              onStart();
            }}
            disabled={!ready}
            className="h-11 px-6 rounded-lg bg-blue-600 text-white text-xs font-bold uppercase tracking-wider hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
          >
            {ready ? 'Start game' : `Need ${minPlayers} players (${members.length}/${minPlayers})`}
          </button>
          {members.length < maxPlayers && (
            <button
              type="button"
              onClick={addBot}
              disabled={adding}
              className="h-11 px-5 rounded-lg border border-violet-300 text-violet-700 text-xs font-bold uppercase tracking-wider hover:bg-violet-50 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
            >
              {adding ? 'Adding…' : '+ Add Jev bot'}
            </button>
          )}
          {botError && <span className="text-xs text-red-700">{botError}</span>}
        </div>
      ) : (
        <p className="text-sm text-slate-600">Waiting for the host to start…</p>
      )}
    </section>
  );
};
