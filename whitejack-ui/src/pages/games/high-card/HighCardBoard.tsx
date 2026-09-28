import React from 'react';
import { CardView } from '@/components/CardView';
import { TableSettings } from '@/types/game';
import { HcView } from '@/types/wire';
import { soundFx } from '@/utils/audio';
import { toUiCard } from '@/utils/cards';

interface HighCardBoardProps {
  view: HcView;
  playerId: string;
  onDraw: () => void;
  settings: TableSettings;
}

/** High Card, rendered entirely from the server's view: this component holds no game rules. */
export const HighCardBoard: React.FC<HighCardBoardProps> = ({ view, playerId, onDraw, settings }) => (
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
          onDraw();
        }}
        className="self-start h-11 px-6 rounded-lg bg-blue-600 text-white text-xs font-bold uppercase tracking-wider hover:bg-blue-700 cursor-pointer"
      >
        Draw
      </button>
    )}
  </section>
);
