import React from 'react';
import { Rank, Suit } from '@/types/game';
import { SUIT_SYMBOLS } from '@/utils/deck';

interface CardFanProps {
  /** Suit of the front-most card — the rest of the fan is fixed dressing. */
  suit: Suit;
  /** `sm` for the lobby grid, `md` for the configure page header. */
  size?: 'sm' | 'md';
  className?: string;
}

/** The three cards sitting behind the game's own card, back to front. */
const BACKDROP: { rank: Rank; suit: Suit }[] = [
  { rank: 'J', suit: 'clubs' },
  { rank: 'Q', suit: 'spades' },
  { rank: 'K', suit: 'diamonds' },
];

const SIZES = {
  sm: {
    box: 'w-16 h-24',
    card: 'w-10 h-14 -ml-5 -mt-7 rounded-md p-0.5',
    corner: 'text-[7px]',
    glyph: 'text-base',
    angle: (i: number) => -22 + i * 9,
    shift: (i: number) => i * 5 - 6,
  },
  md: {
    box: 'w-24 h-28',
    card: 'w-14 h-20 -ml-7 -mt-10 rounded-lg p-1',
    corner: 'text-[10px]',
    glyph: 'text-2xl',
    angle: (i: number) => -26 + i * 11,
    shift: (i: number) => i * 7 - 8,
  },
};

const isRed = (suit: Suit) => suit === 'hearts' || suit === 'diamonds';

const SUIT_BY_GLYPH: Record<string, Suit> = {
  '♥': 'hearts',
  '♠': 'spades',
  '♦': 'diamonds',
  '♣': 'clubs',
};

/** A GameDefinition carries its suit as a glyph; the fan needs the suit itself. */
export const suitFromGlyph = (glyph: string): Suit => SUIT_BY_GLYPH[glyph] ?? 'spades';

/**
 * Decorative fan of playing cards for the lobby game cards. Purely presentational — unlike
 * `CardView` it has no hover, click or disabled states.
 */
export const CardFan: React.FC<CardFanProps> = ({ suit, size = 'md', className = '' }) => {
  const fan = [...BACKDROP, { rank: 'A' as Rank, suit }];
  const s = SIZES[size];

  return (
    <div aria-hidden className={`relative shrink-0 ${s.box} ${className}`}>
      {fan.map((card, i) => {
        const front = i === fan.length - 1;

        return (
          <div
            key={`${card.rank}-${card.suit}`}
            className={`absolute left-1/2 top-1/2 bg-white border flex flex-col justify-between ${s.card} ${
              front ? 'border-slate-200 shadow-md' : 'border-slate-200/80 shadow-sm'
            } ${isRed(card.suit) ? 'text-red-500' : 'text-slate-800'}`}
            style={{ transform: `translateX(${s.shift(i)}px) rotate(${s.angle(i)}deg)` }}
          >
            <span className={`font-space font-bold leading-none ${s.corner}`}>
              {card.rank}
              {SUIT_SYMBOLS[card.suit]}
            </span>
            {front && (
              <span
                className={`absolute inset-0 flex items-center justify-center leading-none ${s.glyph}`}
              >
                {SUIT_SYMBOLS[card.suit]}
              </span>
            )}
            <span className={`font-space font-bold leading-none self-end rotate-180 ${s.corner}`}>
              {card.rank}
              {SUIT_SYMBOLS[card.suit]}
            </span>
          </div>
        );
      })}
    </div>
  );
};
