import React, { useState } from 'react';
import { BOT_NICK_SUFFIX } from '@/network/api';
import { BotDecision, BotHandNotes } from '@/types/wire';

interface BotNotesPanelProps {
  /** Finished hands, newest first, as the server sends them. */
  notes: BotHandNotes[];
  /** Whether any bot is seated; without one the panel has nothing to say. */
  hasBots: boolean;
}

/** Rows shown before "Show all"; a Hearts hand alone is about forty bot moves. */
const COLLAPSED_ROWS = 6;
/** Options drawn as bars per move; the rest are summarised in a count. */
const BARS = 3;

const shortNick = (nick: string) => (nick.endsWith(BOT_NICK_SUFFIX) ? nick.slice(0, -BOT_NICK_SUFFIX.length) : nick);
const capitalize = (s: string) => s.charAt(0).toUpperCase() + s.slice(1);
const percent = (p: number) => `${Math.round(p * 100)}%`;

/** The move in words. Hearts labels are bare cards; Gin and Poker labels already read as actions. */
const describe = (d: BotDecision): string => {
  const labels = d.picked.map((i) => d.options[i]?.label).filter(Boolean);
  if (d.phase === 'play') return `Played ${labels[0]}`;
  if (d.phase === 'pass') return `Passed ${labels.join(' ')}`;
  // Drop the "(adds 80, pot becomes 300)"-style detail; the bar label keeps it in the tooltip.
  return capitalize((labels[0] ?? '').replace(/\s*\(.*\)\s*$/, ''));
};

const sourceLabel = (d: BotDecision): string => {
  switch (d.source) {
    case 'ADVISOR':
      return `Jev · ${d.millis} ms`;
    case 'FORCED':
      return 'Only legal move';
    case 'AFTER_REJECT':
      return 'Built-in move · retry';
    default:
      return d.millis > 0 ? `Built-in move · Jev no answer in ${(d.millis / 1000).toFixed(1)} s` : 'Built-in move';
  }
};

/**
 * How the bots played the last finished hand: each move, what it chose, and — when Jev was
 * asked — its probability for the top options. Released only after the hand, because a bot's
 * options are its own cards.
 */
export const BotNotesPanel: React.FC<BotNotesPanelProps> = ({ notes, hasBots }) => {
  const [expanded, setExpanded] = useState(false);
  if (!hasBots) return null;

  const latest = notes[0];
  const decisions = latest?.decisions ?? [];
  const shown = expanded ? decisions : decisions.slice(0, COLLAPSED_ROWS);

  return (
    <section aria-labelledby="bot-notes-heading" className="border border-stone-200 bg-white rounded-xl p-4 flex flex-col gap-3">
      <div className="flex flex-col gap-0.5">
        <h3 id="bot-notes-heading" className="font-semibold font-display text-stone-900">
          {latest ? `How the bots played hand ${latest.hand + 1}` : 'How the bots play'}
        </h3>
        <p className="text-xs text-stone-500">
          {latest
            ? 'Jev’s probability for each legal move, best first.'
            : 'After each hand, every bot move appears here with the odds Jev gave it. Hidden during the hand so nobody sees a bot’s cards.'}
        </p>
      </div>

      {latest && (
        <ol className="flex flex-col divide-y divide-stone-100">
          {shown.map((d, i) => (
            <DecisionRow key={i} decision={d} />
          ))}
        </ol>
      )}

      {decisions.length > COLLAPSED_ROWS && (
        <button
          type="button"
          onClick={() => setExpanded((e) => !e)}
          aria-expanded={expanded}
          className="self-start text-[13px] font-medium text-stone-600 hover:text-stone-900 cursor-pointer"
        >
          {expanded ? 'Show fewer' : `Show all ${decisions.length} moves`}
        </button>
      )}
    </section>
  );
};

const DecisionRow: React.FC<{ decision: BotDecision }> = ({ decision: d }) => {
  const ranked = d.source === 'ADVISOR' && d.options.every((o) => o.probability != null);
  const picked = new Set(d.picked);
  const order = d.options
    .map((option, index) => ({ ...option, index }))
    .sort((a, b) => (b.probability ?? 0) - (a.probability ?? 0));
  const bars = order.slice(0, Math.max(BARS, d.picked.length));
  const rest = d.options.length - bars.length;

  return (
    <li className="py-2.5 flex flex-col gap-1.5">
      <div className="flex items-baseline justify-between gap-3">
        <span className="text-[13px] text-stone-900 min-w-0 truncate">
          <span className="font-semibold">{shortNick(d.nick)}</span> · {describe(d)}
        </span>
        <span className={`text-[11px] shrink-0 tabular ${d.source === 'ADVISOR' ? 'text-felt-700' : 'text-stone-500'}`}>
          {sourceLabel(d)}
        </span>
      </div>

      {ranked && (
        <ul className="flex flex-col gap-1" aria-label="Options Jev weighed">
          {bars.map((o) => {
            const chosen = picked.has(o.index);
            const p = o.probability ?? 0;
            return (
              <li
                key={o.index}
                title={`${o.label}: ${(p * 100).toFixed(1)}%${chosen ? ' (played)' : ''}`}
                className="grid grid-cols-[minmax(0,6.5rem)_1fr_2.25rem] items-center gap-2 text-[11px]"
              >
                <span className={`truncate ${chosen ? 'text-stone-900 font-medium' : 'text-stone-500'}`}>
                  {o.label.replace(/\s*\(.*\)\s*$/, '')}
                </span>
                <span className="h-1.5 bg-stone-100 rounded-r-[4px]" aria-hidden>
                  <span
                    className={`block h-full rounded-r-[4px] ${chosen ? 'bg-[#2d6a4c]' : 'bg-[#b5aea9]'}`}
                    style={{ width: `max(2px, ${p * 100}%)` }}
                  />
                </span>
                <span className={`text-right tabular ${chosen ? 'text-stone-900 font-medium' : 'text-stone-500'}`}>
                  {percent(p)}
                </span>
              </li>
            );
          })}
          {rest > 0 && <li className="text-[11px] text-stone-400">+{rest} more option{rest === 1 ? '' : 's'}</li>}
        </ul>
      )}
    </li>
  );
};
