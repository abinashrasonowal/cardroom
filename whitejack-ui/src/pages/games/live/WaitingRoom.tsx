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

  const btn = 'h-10 px-4 rounded-md text-sm font-medium transition-[background-color,border-color,transform] active:scale-[0.99] disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer';

  return (
    <section
      aria-labelledby="waiting-heading"
      className="w-full max-w-2xl mx-auto bg-white border border-stone-200 rounded-xl shadow-raised animate-rise"
    >
      <div className="p-6 flex flex-col gap-1.5 border-b border-stone-200">
        <div className="flex items-baseline justify-between gap-4">
          <h2 id="waiting-heading" className="font-display text-xl font-semibold text-stone-900">
            Waiting room
          </h2>
          <span className="text-[13px] text-stone-500 tabular">
            {members.length} of {maxPlayers} seats filled
          </span>
        </div>
        <p className="text-[13px] text-stone-500">
          {ready ? 'Enough players to start.' : `This game needs at least ${minPlayers} players.`}
        </p>
      </div>

      <div className="px-6 py-4 flex flex-wrap items-center justify-between gap-3 bg-stone-50 border-b border-stone-200">
        <div className="flex flex-col gap-0.5 min-w-0">
          <span className="text-[13px] font-medium text-stone-700">Invite link</span>
          <code className="font-mono-code text-[13px] text-stone-500 truncate">
            {window.location.host}/#{room}
          </code>
        </div>
        <button
          type="button"
          onClick={onCopyInvite}
          className={`${btn} h-9 bg-white border border-stone-300 text-stone-900 hover:border-stone-400 flex items-center gap-1.5`}
        >
          <span aria-hidden className="material-symbols-outlined text-[16px]">{copied ? 'check' : 'link'}</span>
          <span aria-live="polite">{copied ? 'Copied' : 'Copy link'}</span>
        </button>
      </div>

      <ul className="px-6 flex flex-col divide-y divide-stone-100">
        {members.map((m) => {
          const bot = isBotNick(m.nick);
          const name = bot ? m.nick.slice(0, -BOT_NICK_SUFFIX.length) : m.nick;
          return (
            <li key={m.id} className="flex items-center justify-between gap-3 py-3">
              <span className="flex items-center gap-3 text-sm text-stone-900 min-w-0">
                <span
                  aria-hidden
                  className="h-8 w-8 shrink-0 rounded-md bg-stone-100 text-stone-600 text-[13px] font-semibold flex items-center justify-center uppercase"
                >
                  {name.slice(0, 1)}
                </span>
                <span className="truncate font-medium">{name}</span>
                {m.id === playerId && <span className="text-[13px] text-stone-500">You</span>}
                {bot && (
                  <span className="text-xs text-stone-600 bg-stone-100 rounded px-1.5 py-0.5 shrink-0">Bot</span>
                )}
              </span>
              <span className="flex items-center gap-3 shrink-0 text-[13px] text-stone-500">
                {m.id === lobby?.host && <span>Host</span>}
                <span
                  title={m.connected ? 'Connected' : 'Away'}
                  className={`h-2 w-2 rounded-full ${m.connected ? 'bg-felt-500' : 'bg-stone-300'}`}
                />
              </span>
            </li>
          );
        })}
      </ul>

      <div className="p-6 border-t border-stone-200">
        {isHost ? (
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={() => {
                soundFx.playCardDeal();
                onStart();
              }}
              disabled={!ready}
              className={`${btn} px-5 bg-stone-900 text-white hover:bg-stone-800`}
            >
              {ready ? 'Start game' : `Waiting for ${minPlayers - members.length} more`}
            </button>
            {members.length < maxPlayers && (
              <button
                type="button"
                onClick={addBot}
                disabled={adding}
                className={`${btn} border border-stone-300 text-stone-900 hover:border-stone-400 bg-white flex items-center gap-1.5`}
              >
                <span aria-hidden className="material-symbols-outlined text-[18px]">add</span>
                {adding ? 'Adding bot…' : 'Add a bot'}
              </button>
            )}
            {botError && (
              <span role="alert" className="text-[13px] text-red-700">
                {botError}
              </span>
            )}
          </div>
        ) : (
          <p role="status" className="text-sm text-stone-500">
            Waiting for the host to start the game…
          </p>
        )}
      </div>
    </section>
  );
};
