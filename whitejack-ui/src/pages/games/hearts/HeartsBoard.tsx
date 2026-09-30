import React, { useEffect, useState } from 'react';
import { CardView } from '@/components/CardView';
import { TableSettings } from '@/types/game';
import { HeartsPlay, HeartsSeat, HeartsView, WireCard } from '@/types/wire';
import { soundFx } from '@/utils/audio';
import { sortHand, toUiCard } from '@/utils/cards';

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

  const statusActive = myTurn || (canPass && !me?.passed);

  return (
    // Fills the viewport between navbar and footer on wide screens: table and hand on the left,
    // status and scores on the right.
    <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_20rem] xl:grid-cols-[minmax(0,1fr)_22rem] items-start">
      <div className="flex flex-col gap-4 min-w-0 lg:min-h-[calc(100dvh-15rem)]">
        {/* Table: all four seats round the trick */}
        <section className="flex-1 rounded-2xl bg-slate-900 p-3 sm:p-5 grid grid-cols-[1fr_auto_1fr] grid-rows-[auto_1fr_auto] items-center gap-3">
          <div className="col-start-2 row-start-1 justify-self-center">
            <SeatBadge seat={seatAt('top')} view={view} />
          </div>
          {/* On phones the three opponents sit in one row above the trick; wider, they surround it. */}
          <div className="col-start-1 row-start-1 sm:row-start-2 justify-self-start">
            <SeatBadge seat={seatAt('left')} view={view} />
          </div>
          <div className="col-start-3 row-start-1 sm:row-start-2 justify-self-end">
            <SeatBadge seat={seatAt('right')} view={view} />
          </div>

          <div className="col-span-3 sm:col-span-1 sm:col-start-2 row-start-2 justify-self-center relative w-56 h-60 sm:w-72 sm:h-68 md:h-72 rounded-2xl bg-slate-800/60 border border-slate-700/70">
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

          <div className="col-span-3 row-start-3 justify-self-center">
            <SeatBadge seat={me} view={view} you />
          </div>
        </section>

        {/* Your cards */}
        <section
          className={`rounded-2xl border px-3 sm:px-5 pt-3 pb-4 flex flex-col gap-1 bg-slate-900 ${
            statusActive ? 'border-blue-400 ring-2 ring-blue-400/60' : 'border-slate-900'
          }`}
        >
          <div className="flex flex-wrap items-center justify-between gap-2 min-h-9">
            <span className="text-sm font-semibold text-white">Your hand</span>
            {canPass && (
              <button
                type="button"
                onClick={handlePass}
                disabled={selected.length !== 3}
                className="h-9 px-4 rounded-lg bg-blue-600 text-white text-xs font-bold uppercase tracking-wider hover:bg-blue-500 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
              >
                Pass {selected.length}/3 {DIRECTION_LABEL[view.passDirection]}
              </button>
            )}
          </div>
          {/* Overlapped like a held fan; the top padding leaves room for a picked card to lift. */}
          <div className="flex justify-center pt-5 overflow-x-auto">
            {sortHand(view.myHand).map((card) => {
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
                  className="-ml-11 sm:-ml-8 lg:-ml-9 xl:-ml-4 first:ml-0"
                />
              );
            })}
            {view.myHand.length === 0 && <span className="text-sm text-slate-400 py-8">No cards.</span>}
          </div>
        </section>
      </div>

      {/* Sidebar: where the hand stands, then the scores */}
      <aside className="flex flex-col gap-4 lg:sticky lg:top-28">
        <section className="border border-slate-200 bg-white rounded-2xl p-4 flex flex-col gap-3">
          <div className="flex items-center justify-between gap-2 text-sm">
            <span className="font-bold font-space text-slate-950">Hand {view.hand + 1}</span>
            <span className="text-slate-600">
              {passing ? `Passing ${DIRECTION_LABEL[view.passDirection]}` : `Trick ${Math.min(view.tricksPlayed + 1, 13)} / 13`}
            </span>
          </div>
          <div className="flex flex-wrap gap-1.5">
            {view.passDirection === 'HOLD' && view.phase === 'PLAYING' && (
              <span className="text-xs text-slate-600 bg-slate-100 border border-slate-200 px-2 py-0.5 rounded-full">
                Hold hand — no passing
              </span>
            )}
            {view.heartsBroken && (
              <span className="text-xs font-semibold text-red-700 bg-red-50 border border-red-200 px-2 py-0.5 rounded-full">
                ♥ Hearts broken
              </span>
            )}
          </div>
          <p
            className={`text-sm font-semibold rounded-lg px-3 py-2 ${
              statusActive ? 'bg-blue-50 text-blue-700' : 'bg-slate-50 text-slate-600'
            }`}
          >
            {status()}
          </p>
        </section>

        {view.phase === 'GAME_OVER' && (
          <section className="rounded-2xl border border-amber-300 bg-amber-50 p-4 font-bold font-space text-slate-950">
            🏆 {nickOf(view.winner)} wins with {view.seats.find((s) => s.id === view.winner)?.score} points
          </section>
        )}

        <section className="border border-slate-200 bg-white rounded-2xl p-4 flex flex-col gap-2">
          <h3 className="font-bold font-space text-slate-950">Scores</h3>
          {/* One row per player, so four names never overflow the sidebar however many hands are played. */}
          <ul className="flex flex-col divide-y divide-slate-100">
            {view.seats.map((s, j) => (
              <li key={s.id} className="py-2 flex items-center justify-between gap-3">
                <div className="flex flex-col min-w-0">
                  <span className={`text-sm font-semibold truncate ${s.id === playerId ? 'text-blue-700' : 'text-slate-900'}`}>
                    {s.nick}
                    {s.id === playerId && <span className="text-xs font-normal text-slate-500"> (you)</span>}
                  </span>
                  {view.history.length > 0 && (
                    <span className="text-xs text-slate-500 font-mono-code truncate">
                      {view.history.map((row) => row[j]).join(' · ')}
                    </span>
                  )}
                </div>
                <span className="text-lg font-bold font-mono-code text-slate-950">{s.score}</span>
              </li>
            ))}
          </ul>
        </section>
      </aside>
    </div>
  );
};

const TRICK_POSITION: Record<Spot, string> = {
  bottom: 'bottom-2 left-1/2 -translate-x-1/2',
  top: 'top-2 left-1/2 -translate-x-1/2',
  left: 'left-2 top-1/2 -translate-y-1/2',
  right: 'right-2 top-1/2 -translate-y-1/2',
};

const SeatBadge: React.FC<{ seat: HeartsSeat | undefined; view: HeartsView; you?: boolean }> = ({
  seat,
  view,
  you = false,
}) => {
  if (!seat) return null;
  const onClock = seat.id === view.onClock && view.phase === 'PLAYING';
  return (
    <div
      className={`rounded-xl px-2.5 sm:px-3 py-2 text-white border min-w-0 ${
        onClock ? 'border-blue-400 bg-blue-600/30 ring-2 ring-blue-400' : 'border-slate-700 bg-slate-800'
      }`}
    >
      <div className="flex items-center gap-2 text-sm font-semibold truncate">
        {seat.nick}
        {you && <span className="text-xs font-normal text-slate-300">(you)</span>}
        {view.phase === 'PASSING' && seat.passed && <span className="text-xs text-emerald-300">✓ passed</span>}
      </div>
      <div className="text-[11px] sm:text-xs text-slate-300">
        {seat.cardCount} cards · ♥ {seat.handPoints} · {seat.score} pts
      </div>
    </div>
  );
};
