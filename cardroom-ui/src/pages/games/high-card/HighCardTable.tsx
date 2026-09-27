import React, { useState } from 'react';
import { CardView } from '@/components/CardView';
import { useRoom } from '@/network/useRoom';
import { TableSettings } from '@/types/game';
import { HcView } from '@/types/wire';
import { soundFx } from '@/utils/audio';
import { toUiCard } from '@/utils/cards';

interface HighCardTableProps {
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

/** High Card, rendered entirely from server views: this component holds no game rules. */
export const HighCardTable: React.FC<HighCardTableProps> = ({ room, nick, playerId, token, settings, onLeave }) => {
  const { status, lobby, game, error, fatal, start, intent, leave, closeRoom } = useRoom(room, nick, token);
  const [copied, setCopied] = useState(false);
  const view = game as HcView | null;
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

  return (
    <div className="w-full max-w-4xl mx-auto px-4 sm:px-8 flex flex-col gap-6">
      <section className="bg-slate-50 border border-slate-200 rounded-2xl p-5 flex flex-wrap items-center justify-between gap-4">
        <div className="flex flex-col gap-1">
          <span className="text-xs text-blue-600 uppercase font-bold tracking-widest">High Card · Live</span>
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

      {!view ? (
        <section className="border border-slate-200 rounded-2xl p-6 flex flex-col gap-4">
          <h2 className="text-xl font-bold font-space text-slate-950">Waiting room</h2>
          <ul className="flex flex-col divide-y divide-slate-100">
            {(lobby?.members ?? []).map((m) => (
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
                start();
              }}
              disabled={(lobby?.members.length ?? 0) < 2}
              className="self-start h-11 px-6 rounded-lg bg-blue-600 text-white text-xs font-bold uppercase tracking-wider hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
            >
              {(lobby?.members.length ?? 0) < 2 ? 'Need 2 players to start' : 'Start game'}
            </button>
          ) : (
            <p className="text-sm text-slate-600">Waiting for the host to start…</p>
          )}
        </section>
      ) : (
        <section className="border border-slate-200 rounded-2xl p-6 flex flex-col gap-6">
          {view.handComplete ? (
            <p className="text-lg font-bold font-space text-slate-950">
              🏆 {view.seats.find((s) => s.id === view.winner)?.nick ?? 'Nobody'} wins the hand
            </p>
          ) : (
            <p className="text-sm text-slate-600">
              {view.onClock === playerId
                ? 'Your turn — draw a card.'
                : `Waiting on ${view.seats.find((s) => s.id === view.onClock)?.nick ?? '…'}`}
            </p>
          )}

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            {view.seats.map((seat) => (
              <div
                key={seat.id}
                className={`flex flex-col items-center gap-2 rounded-xl p-3 border ${
                  seat.id === view.winner
                    ? 'border-amber-300 bg-amber-50'
                    : seat.id === view.onClock
                      ? 'border-blue-300 bg-blue-50'
                      : 'border-slate-200'
                }`}
              >
                {seat.hasDrawn ? (
                  <CardView
                    card={seat.card ? toUiCard(seat.card) : undefined}
                    faceDown={!seat.card}
                    cardBack={settings.cardBack}
                    fourColor={settings.fourColorDeck}
                  />
                ) : (
                  <div className="w-16 h-23 sm:w-18 sm:h-26 md:w-20 md:h-28 rounded-lg border-2 border-dashed border-slate-300" />
                )}
                <span className="text-sm font-semibold text-slate-900">
                  {seat.nick}
                  {seat.id === playerId && <span className="text-xs text-slate-500"> (you)</span>}
                </span>
              </div>
            ))}
          </div>

          {view.onClock === playerId && (
            <button
              type="button"
              onClick={() => {
                soundFx.playCardDeal();
                intent({ type: 'draw' });
              }}
              className="self-start h-11 px-6 rounded-lg bg-blue-600 text-white text-xs font-bold uppercase tracking-wider hover:bg-blue-700 cursor-pointer"
            >
              Draw
            </button>
          )}
        </section>
      )}
    </div>
  );
};
