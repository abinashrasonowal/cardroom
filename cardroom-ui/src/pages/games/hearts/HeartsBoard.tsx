import React, { useEffect, useState } from 'react';
import { CardView } from '@/components/CardView';
import { TableSettings } from '@/types/game';
import { HeartsPlay, HeartsSeat, HeartsView, WireCard } from '@/types/wire';
import { soundFx } from '@/utils/audio';
import { toUiCard } from '@/utils/cards';

interface HeartsBoardProps {
  view: HeartsView;
  playerId: string;
  intent: (payload: object) => string;
  settings: TableSettings;
}

type Spot = 'bottom' | 'left' | 'top' | 'right';

/** Seat offsets from the viewer, clockwise: you at the bottom, the player you pass left to on the left. */
const SPOTS: Spot[] = ['bottom', 'left', 'top', 'right'];

const DIRECTION_LABEL = { LEFT: 'left', RIGHT: 'right', ACROSS: 'across', HOLD: 'hold' } as const;

const cardKey = (card: WireCard) => `${card.rank}-${card.suit}`;
const sameCard = (a: WireCard, b: WireCard) => a.rank === b.rank && a.suit === b.suit;

/**
 * Hearts, rendered entirely from the server's view. Which cards are playable comes from
 * `view.legal`, so this component enables buttons but never decides a rule.
 */
export const HeartsBoard: React.FC<HeartsBoardProps> = ({ view, playerId, intent, settings }) => {
  const [selected, setSelected] = useState<WireCard[]>([]);

  // A new hand, or the end of passing, clears whatever was picked.
  useEffect(() => setSelected([]), [view.hand, view.phase]);

  const me = view.seats.find((s) => s.id === playerId);
  const myIndex = me?.index ?? 0;
  const seatAt = (spot: Spot) => view.seats[(myIndex + SPOTS.indexOf(spot)) % view.seats.length];
  const nickOf = (id: string | null) => view.seats.find((s) => s.id === id)?.nick ?? '…';
  const spotOf = (id: string) => SPOTS[(view.seats.findIndex((s) => s.id === id) - myIndex + 4) % 4];

  const passing = view.phase === 'PASSING';
  const myTurn = view.phase === 'PLAYING' && view.onClock === playerId;
  const canPass = passing && view.legal.length > 0;
  const isLegal = (card: WireCard) => view.legal.some((c) => sameCard(c, card));
  const isSelected = (card: WireCard) => selected.some((c) => sameCard(c, card));
  const passTarget = () => {
    const offset = { LEFT: 1, RIGHT: 3, ACROSS: 2, HOLD: 0 }[view.passDirection];
    return view.seats[(myIndex + offset) % view.seats.length]?.nick;
  };

  const handleCardClick = (card: WireCard) => {
    if (canPass) {
      soundFx.playClick();
      setSelected((prev) =>
        isSelected(card) ? prev.filter((c) => !sameCard(c, card)) : prev.length < 3 ? [...prev, card] : prev
      );
    } else if (myTurn && isLegal(card)) {
      soundFx.playCardDeal();
      intent({ type: 'play', card });
    }
  };

  const handlePass = () => {
    soundFx.playCardDeal();
    intent({ type: 'pass', cards: selected });
  };

  // An empty trick shows the one just taken, so its fourth card is not lost the instant it lands.
  const showingLast = view.trick.length === 0 && view.lastTrick.length > 0;
  const tablePlays: HeartsPlay[] = showingLast ? view.lastTrick : view.trick;

  const status = () => {
    if (view.phase === 'GAME_OVER') return 'Game over';
    if (passing) {
      return me?.passed ? 'Waiting for the others to pass…' : `Choose 3 cards to pass ${DIRECTION_LABEL[view.passDirection]} to ${passTarget()}`;
    }
    return myTurn ? 'Your turn — play a highlighted card' : `Waiting on ${nickOf(view.onClock)}`;
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Status bar */}
      <section className="border border-slate-200 rounded-2xl px-5 py-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-3 text-sm">
          <span className="font-bold font-space text-slate-950">Hand {view.hand + 1}</span>
          <span className="text-slate-300">•</span>
          <span className="text-slate-700">
            {passing ? `Passing ${DIRECTION_LABEL[view.passDirection]}` : `Trick ${Math.min(view.tricksPlayed + 1, 13)} / 13`}
          </span>
          {view.passDirection === 'HOLD' && !passing && view.phase === 'PLAYING' && (
            <span className="text-xs text-slate-500">(hold hand — no passing)</span>
          )}
          {view.heartsBroken && (
            <span className="text-xs font-semibold text-red-700 bg-red-50 border border-red-200 px-2 py-0.5 rounded-full">
              ♥ Hearts broken
            </span>
          )}
        </div>
        <span className={`text-sm font-semibold ${myTurn || (canPass && !me?.passed) ? 'text-blue-700' : 'text-slate-600'}`}>
          {status()}
        </span>
      </section>

      {view.phase === 'GAME_OVER' && (
        <section className="rounded-2xl border border-amber-300 bg-amber-50 px-5 py-4 text-lg font-bold font-space text-slate-950">
          🏆 {nickOf(view.winner)} wins with {view.seats.find((s) => s.id === view.winner)?.score} points
        </section>
      )}

      {/* Table */}
      <section className="rounded-2xl bg-slate-900 p-4 sm:p-6 grid grid-cols-[1fr_auto_1fr] grid-rows-[auto_auto_auto] gap-4 items-center">
        <div className="col-start-2 row-start-1 justify-self-center">
          <SeatBadge seat={seatAt('top')} view={view} playerId={playerId} />
        </div>
        <div className="col-start-1 row-start-2 justify-self-start">
          <SeatBadge seat={seatAt('left')} view={view} playerId={playerId} />
        </div>
        <div className="col-start-3 row-start-2 justify-self-end">
          <SeatBadge seat={seatAt('right')} view={view} playerId={playerId} />
        </div>

        <div className="col-start-2 row-start-2 relative w-64 h-64 sm:w-72 sm:h-72 rounded-full bg-slate-800/70 border border-slate-700">
          {tablePlays.map((play) => (
            <div
              key={cardKey(play.card)}
              className={`absolute ${TRICK_POSITION[spotOf(play.player)]} ${showingLast ? 'opacity-50' : ''}`}
            >
              <CardView card={toUiCard(play.card)} cardBack={settings.cardBack} fourColor={settings.fourColorDeck} />
            </div>
          ))}
          {showingLast && (
            <span className="absolute inset-x-0 top-1/2 -translate-y-1/2 text-center text-xs font-semibold text-white">
              won by {view.lastTrickWinner === playerId ? 'you' : nickOf(view.lastTrickWinner)}
            </span>
          )}
        </div>

        <div className="col-start-2 row-start-3 justify-self-center">
          <SeatBadge seat={seatAt('bottom')} view={view} playerId={playerId} />
        </div>
      </section>

      {/* My hand */}
      <section className="border border-slate-200 rounded-2xl p-5 flex flex-col gap-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <span className="font-bold font-space text-slate-950">Your hand</span>
          {canPass && (
            <button
              type="button"
              onClick={handlePass}
              disabled={selected.length !== 3}
              className="h-10 px-5 rounded-lg bg-blue-600 text-white text-xs font-bold uppercase tracking-wider hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
            >
              Pass {selected.length}/3 {DIRECTION_LABEL[view.passDirection]}
            </button>
          )}
        </div>
        <div className="flex flex-wrap gap-2">
          {view.myHand.map((card) => {
            const clickable = canPass || (myTurn && isLegal(card));
            return (
              <CardView
                key={cardKey(card)}
                card={toUiCard(card)}
                selected={isSelected(card)}
                disabled={myTurn && !isLegal(card)}
                onClick={clickable ? () => handleCardClick(card) : undefined}
                cardBack={settings.cardBack}
                fourColor={settings.fourColorDeck}
              />
            );
          })}
          {view.myHand.length === 0 && <span className="text-sm text-slate-500">No cards.</span>}
        </div>
      </section>

      {/* Scores */}
      <section className="border border-slate-200 rounded-2xl p-5 overflow-x-auto">
        <table className="w-full text-sm text-left">
          <thead>
            <tr className="text-xs uppercase text-slate-500">
              <th className="py-2 pr-3 font-semibold">Hand</th>
              {view.seats.map((s) => (
                <th key={s.id} className={`py-2 px-2 text-center font-semibold ${s.id === playerId ? 'text-blue-700' : ''}`}>
                  {s.nick}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 font-mono-code">
            {view.history.map((row, i) => (
              <tr key={i}>
                <td className="py-1.5 pr-3 text-slate-500">{i + 1}</td>
                {row.map((points, j) => (
                  <td key={j} className="py-1.5 px-2 text-center">
                    {points}
                  </td>
                ))}
              </tr>
            ))}
            <tr className="font-bold">
              <td className="py-2 pr-3 text-slate-700">Total</td>
              {view.seats.map((s) => (
                <td key={s.id} className="py-2 px-2 text-center">
                  {s.score}
                </td>
              ))}
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  );
};

const TRICK_POSITION: Record<Spot, string> = {
  bottom: 'bottom-3 left-1/2 -translate-x-1/2',
  top: 'top-3 left-1/2 -translate-x-1/2',
  left: 'left-3 top-1/2 -translate-y-1/2',
  right: 'right-3 top-1/2 -translate-y-1/2',
};

const SeatBadge: React.FC<{ seat: HeartsSeat | undefined; view: HeartsView; playerId: string }> = ({
  seat,
  view,
  playerId,
}) => {
  if (!seat) return null;
  const onClock = seat.id === view.onClock && view.phase === 'PLAYING';
  return (
    <div
      className={`rounded-xl px-3.5 py-2 text-white border ${
        onClock ? 'border-blue-400 bg-blue-600/30 ring-2 ring-blue-400' : 'border-slate-700 bg-slate-800'
      }`}
    >
      <div className="flex items-center gap-2 text-sm font-semibold">
        {seat.nick}
        {seat.id === playerId && <span className="text-xs text-slate-300">(you)</span>}
        {view.phase === 'PASSING' && seat.passed && <span className="text-xs text-emerald-300">✓ passed</span>}
      </div>
      <div className="text-xs text-slate-300">
        {seat.cardCount} cards · ♥ {seat.handPoints} this hand · {seat.score} total
      </div>
    </div>
  );
};
