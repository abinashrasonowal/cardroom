import React, { useState } from 'react';
import { GAME_DEFINITIONS } from '@/config/games';
import { useRoom } from '@/network/useRoom';
import { HeartsBoard } from '@/pages/games/hearts/HeartsBoard';
import { HighCardBoard } from '@/pages/games/high-card/HighCardBoard';
import { TableSettings } from '@/types/game';
import { HcView, HeartsView } from '@/types/wire';
import { soundFx } from '@/utils/audio';
import { WaitingRoom } from './WaitingRoom';

interface LiveTableProps {
  room: string;
  nick: string;
  playerId: string;
  token: string;
  settings: TableSettings;
  onLeave: () => void;
}

const STATUS_LABEL = {
  connecting: 'Connecting…',
  open: 'Live',
  reconnecting: 'Reconnecting…',
  closed: 'Disconnected',
} as const;

/**
 * Every server-backed room: the room bar, errors, the waiting room, then the board for whichever
 * game the server says this room plays. Boards receive views and send intents; none holds rules.
 */
export const LiveTable: React.FC<LiveTableProps> = ({ room, nick, playerId, token, settings, onLeave }) => {
  const { status, lobby, game, gameId, error, fatal, start, intent, leave, closeRoom } = useRoom(room, nick, token);
  const [copied, setCopied] = useState(false);
  const def = Object.values(GAME_DEFINITIONS).find((d) => d.serverGameId === gameId);
  const isHost = lobby?.host === playerId;

  const copyInvite = () => {
    navigator.clipboard?.writeText(`${window.location.origin}/#${room}`).catch(() => {});
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleLeave = () => {
    soundFx.playClick();
    leave();
    onLeave();
  };

  const handleClose = () => {
    soundFx.playClick();
    closeRoom();
    onLeave();
  };

  const board = () => {
    if (gameId === 'hearts') {
      return <HeartsBoard view={game as HeartsView} playerId={playerId} intent={intent} settings={settings} />;
    }
    if (gameId === 'high-card') {
      return (
        <HighCardBoard
          view={game as HcView}
          playerId={playerId}
          onDraw={() => intent({ type: 'draw' })}
          settings={settings}
        />
      );
    }
    return <p className="text-sm text-slate-600">This client cannot render “{gameId}” yet.</p>;
  };

  return (
    <div className="w-full max-w-6xl mx-auto px-4 sm:px-8 flex flex-col gap-6">
      <section className="bg-slate-50 border border-slate-200 rounded-2xl p-5 flex flex-wrap items-center justify-between gap-4">
        <div className="flex flex-col gap-1">
          <span className="text-xs text-blue-600 uppercase font-bold tracking-widest">
            {def?.name ?? 'Live room'} · Live
          </span>
          <div className="flex items-center gap-3">
            <code className="text-2xl font-mono-code font-bold tracking-wider text-slate-950">{room}</code>
            <button
              type="button"
              onClick={copyInvite}
              className="text-xs font-semibold text-blue-600 hover:text-blue-700 cursor-pointer"
            >
              {copied ? '✓ Invite link copied' : 'Copy invite link'}
            </button>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <span
            className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${
              status === 'open'
                ? 'text-emerald-700 bg-emerald-50 border-emerald-200'
                : 'text-amber-700 bg-amber-50 border-amber-200'
            }`}
          >
            {STATUS_LABEL[status]}
          </span>
          {isHost && (
            <button
              type="button"
              onClick={handleClose}
              className="h-9 px-4 rounded-lg border border-slate-300 text-xs font-bold uppercase tracking-wider text-slate-700 hover:bg-white cursor-pointer"
            >
              Close room
            </button>
          )}
          <button
            type="button"
            onClick={handleLeave}
            className="h-9 px-4 rounded-lg border border-slate-300 text-xs font-bold uppercase tracking-wider text-slate-700 hover:bg-white cursor-pointer"
          >
            Leave
          </button>
        </div>
      </section>

      {error && (
        <div
          role="alert"
          className={`rounded-xl border px-4 py-3 text-sm ${
            fatal ? 'border-red-200 bg-red-50 text-red-800' : 'border-amber-200 bg-amber-50 text-amber-800'
          }`}
        >
          <strong className="font-mono-code text-xs mr-2">{error.error}</strong>
          {error.detail}
        </div>
      )}

      {game ? (
        board()
      ) : (
        <WaitingRoom lobby={lobby} playerId={playerId} minPlayers={def?.playersCount ?? 2} onStart={start} />
      )}
    </div>
  );
};
