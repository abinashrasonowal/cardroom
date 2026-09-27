import React, { useEffect, useState } from 'react';
import { CardView } from '@/components/CardView';
import { TableSettings } from '@/types/game';
import { GinHandResult, GinView, WireCard } from '@/types/wire';
import { soundFx } from '@/utils/audio';
import { toUiCard } from '@/utils/cards';

interface GinRummyBoardProps {
  view: GinView;
  playerId: string;
  intent: (payload: object) => string;
  settings: TableSettings;
}

const cardKey = (card: WireCard) => `${card.rank}-${card.suit}`;
const sameCard = (a: WireCard | null, b: WireCard | null) => !!a && !!b && a.rank === b.rank && a.suit === b.suit;
const contains = (cards: WireCard[], card: WireCard | null) => cards.some((c) => sameCard(c, card));

/**
 * Gin Rummy, rendered from the server's view. Every button is enabled from the view's legal-move
 * lists (`drawSources`, `discards`, `knockDiscards`, `ginDiscards`); this component decides no rule.
 */
export const GinRummyBoard: React.FC<GinRummyBoardProps> = ({ view, playerId, intent, settings }) => {
  const [selected, setSelected] = useState<WireCard | null>(null);

  // A new turn phase or a new hand clears the pick.
  useEffect(() => setSelected(null), [view.phase, view.hand, view.onClock]);

  const me = view.seats.find((s) => s.id === playerId);
  const opponent = view.seats.find((s) => s.id !== playerId);
  const nickOf = (id: string | null) => (id === playerId ? 'You' : view.seats.find((s) => s.id === id)?.nick ?? '…');
  const myTurn = view.onClock === playerId;
  const drawing = myTurn && view.phase === 'DRAW';
  const discarding = myTurn && view.phase === 'DISCARD';

  const draw = (source: 'stock' | 'discard') => {
    soundFx.playCardDeal();
    intent({ type: 'draw', source });
  };

  const discard = (knock: boolean) => {
    if (!selected) return;
    soundFx.playCardDeal();
    intent({ type: 'discard', card: selected, knock });
  };

  const handleCardClick = (card: WireCard) => {
    if (!discarding || !contains(view.discards, card)) return;
    soundFx.playClick();
    setSelected((prev) => (sameCard(prev, card) ? null : card));
  };

  const status = () => {
    if (view.phase === 'GAME_OVER') return 'Game over';
    if (drawing) return 'Your turn — draw from the stock or the discard pile';
    if (discarding) return selected ? 'Discard it, or knock if you can' : 'Pick a card to discard';
    return `Waiting on ${nickOf(view.onClock)}`;
  };

  const myCard = (card: WireCard) => (
    <CardView
      key={cardKey(card)}
      card={toUiCard(card)}
      selected={sameCard(selected, card)}
      disabled={discarding && !contains(view.discards, card)}
      onClick={discarding && contains(view.discards, card) ? () => handleCardClick(card) : undefined}
      tooltip={sameCard(view.takenFromDiscard, card) ? 'Just taken from the pile — cannot be discarded this turn' : undefined}
      cardBack={settings.cardBack}
      fourColor={settings.fourColorDeck}
    />
  );

  return (
    <div className="flex flex-col gap-6">
      {/* Status bar */}
      <section className="border border-slate-200 rounded-2xl px-5 py-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-3 text-sm">
          <span className="font-bold font-space text-slate-950">Hand {view.hand + 1}</span>
          <span className="text-slate-300">•</span>
          <span className="text-slate-700">{nickOf(view.dealer)} dealt</span>
          <span className="text-slate-300">•</span>
          <span className="text-slate-700">First to 100</span>
        </div>
        <span className={`text-sm font-semibold ${myTurn ? 'text-blue-700' : 'text-slate-600'}`}>{status()}</span>
      </section>

      {view.phase === 'GAME_OVER' && (
        <section className="rounded-2xl border border-amber-300 bg-amber-50 px-5 py-4 text-lg font-bold font-space text-slate-950">
          🏆 {nickOf(view.winner)} {view.winner === playerId ? 'win' : 'wins'} with{' '}
          {view.seats.find((s) => s.id === view.winner)?.score} points
        </section>
      )}

      {/* Table */}
      <section className="rounded-2xl bg-slate-900 p-5 sm:p-6 flex flex-col items-center gap-6 text-white">
        {opponent && (
          <div
            className={`rounded-xl px-4 py-2 border ${
              view.onClock === opponent.id ? 'border-blue-400 ring-2 ring-blue-400 bg-blue-600/30' : 'border-slate-700 bg-slate-800'
            }`}
          >
            <div className="text-sm font-semibold">{opponent.nick}</div>
            <div className="text-xs text-slate-300">
              {opponent.cardCount} cards · {opponent.score} points
            </div>
          </div>
        )}

        <div className="flex items-end gap-10">
          <div className="flex flex-col items-center gap-2">
            <button
              type="button"
              disabled={!view.drawSources.includes('STOCK')}
              onClick={() => draw('stock')}
              className={`rounded-lg ${view.drawSources.includes('STOCK') ? 'ring-2 ring-blue-400 cursor-pointer hover:-translate-y-1 transition-transform' : 'cursor-default'}`}
              aria-label="Draw from the stock"
            >
              <CardView faceDown cardBack={settings.cardBack} />
            </button>
            <span className="text-xs text-slate-300">Stock · {view.stockCount}</span>
          </div>
          <div className="flex flex-col items-center gap-2">
            <button
              type="button"
              disabled={!view.drawSources.includes('DISCARD')}
              onClick={() => draw('discard')}
              className={`rounded-lg ${view.drawSources.includes('DISCARD') ? 'ring-2 ring-blue-400 cursor-pointer hover:-translate-y-1 transition-transform' : 'cursor-default'}`}
              aria-label="Take the top discard"
            >
              {view.discardTop ? (
                <CardView card={toUiCard(view.discardTop)} cardBack={settings.cardBack} fourColor={settings.fourColorDeck} />
              ) : (
                <div className="w-16 h-23 sm:w-18 sm:h-26 md:w-20 md:h-28 rounded-lg border-2 border-dashed border-slate-600" />
              )}
            </button>
            <span className="text-xs text-slate-300">Discard · {view.discardCount}</span>
          </div>
        </div>

        {me && (
          <div
            className={`rounded-xl px-4 py-2 border ${
              myTurn ? 'border-blue-400 ring-2 ring-blue-400 bg-blue-600/30' : 'border-slate-700 bg-slate-800'
            }`}
          >
            <div className="text-sm font-semibold">
              {me.nick} <span className="text-xs text-slate-300">(you)</span>
            </div>
            <div className="text-xs text-slate-300">{me.score} points</div>
          </div>
        )}
      </section>

      {/* My hand, grouped the way it would be laid down */}
      <section className="border border-slate-200 rounded-2xl p-5 flex flex-col gap-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <span className="font-bold font-space text-slate-950">
            Your hand
            {view.myMelds && (
              <span className="ml-2 text-sm font-normal text-slate-600">deadwood {view.myMelds.deadwoodPoints}</span>
            )}
          </span>
          {discarding && (
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => discard(false)}
                disabled={!selected}
                className="h-10 px-5 rounded-lg bg-slate-800 text-white text-xs font-bold uppercase tracking-wider hover:bg-slate-900 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
              >
                Discard
              </button>
              {contains(view.ginDiscards, selected) ? (
                <button
                  type="button"
                  onClick={() => discard(true)}
                  className="h-10 px-5 rounded-lg bg-emerald-600 text-white text-xs font-bold uppercase tracking-wider hover:bg-emerald-700 cursor-pointer"
                >
                  Gin!
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => discard(true)}
                  disabled={!contains(view.knockDiscards, selected)}
                  title={view.knockDiscards.length === 0 ? 'Knocking needs 10 or less deadwood after your discard' : undefined}
                  className="h-10 px-5 rounded-lg bg-blue-600 text-white text-xs font-bold uppercase tracking-wider hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                >
                  Knock
                </button>
              )}
            </div>
          )}
        </div>

        {view.myMelds ? (
          <div className="flex flex-wrap gap-4">
            {view.myMelds.melds.map((meld) => (
              <div key={meld.map(cardKey).join()} className="flex gap-1.5 rounded-xl border border-emerald-200 bg-emerald-50/60 p-2">
                {meld.map(myCard)}
              </div>
            ))}
            {view.myMelds.deadwood.length > 0 && (
              <div className="flex flex-wrap gap-1.5 rounded-xl border border-dashed border-slate-300 p-2">
                {view.myMelds.deadwood.map(myCard)}
              </div>
            )}
          </div>
        ) : (
          <span className="text-sm text-slate-500">Watching.</span>
        )}
        <p className="text-xs text-slate-500">
          Green groups are melds. Cards in the dashed group count as deadwood.
        </p>
      </section>

      {view.lastHand && <LastHand result={view.lastHand} nickOf={nickOf} settings={settings} />}

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

const OUTCOME_LABEL = { KNOCK: 'knocked', GIN: 'went gin', UNDERCUT: 'knocked and was undercut', DEAD: '' } as const;

/** Both hands as they were laid down at the end of the previous hand. */
const LastHand: React.FC<{ result: GinHandResult; nickOf: (id: string | null) => string; settings: TableSettings }> = ({
  result,
  nickOf,
  settings,
}) => {
  const small = (card: WireCard) => (
    <CardView key={cardKey(card)} card={toUiCard(card)} size="sm" cardBack={settings.cardBack} fourColor={settings.fourColorDeck} />
  );
  return (
    <section className="border border-slate-200 rounded-2xl p-5 flex flex-col gap-4">
      <h3 className="font-bold font-space text-slate-950">
        Last hand:{' '}
        {result.outcome === 'DEAD'
          ? 'the stock ran out — no score'
          : `${nickOf(result.knocker)} ${OUTCOME_LABEL[result.outcome]}. ${nickOf(result.winner)} scored ${result.points}.`}
      </h3>
      {result.hands.map((hand) => (
        <div key={hand.player} className="flex flex-col gap-2">
          <span className="text-sm font-semibold text-slate-700">
            {nickOf(hand.player)} · deadwood {hand.deadwoodPoints}
          </span>
          <div className="flex flex-wrap items-center gap-3">
            {hand.melds.map((meld) => (
              <div key={meld.map(cardKey).join()} className="flex gap-1 rounded-lg border border-emerald-200 bg-emerald-50/60 p-1.5">
                {meld.map(small)}
              </div>
            ))}
            {hand.laidOff.length > 0 && (
              <div className="flex items-center gap-1 rounded-lg border border-blue-200 bg-blue-50/60 p-1.5">
                <span className="text-[10px] font-semibold text-blue-700 px-1">laid off</span>
                {hand.laidOff.map(small)}
              </div>
            )}
            {hand.deadwood.length > 0 && (
              <div className="flex gap-1 rounded-lg border border-dashed border-slate-300 p-1.5">{hand.deadwood.map(small)}</div>
            )}
          </div>
        </div>
      ))}
    </section>
  );
};
