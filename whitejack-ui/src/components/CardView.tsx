import React from 'react';
import { Card, Rank, Suit } from '@/types/game';

interface CardViewProps {
  card?: Card;
  faceDown?: boolean;
  selected?: boolean;
  disabled?: boolean;
  onClick?: () => void;
  size?: 'sm' | 'md' | 'lg';
  fourColor?: boolean;
  cardBack?: 'geometric-blue' | 'classic-cross' | 'crimson-diamond';
  className?: string;
  tooltip?: string;
}

// Faces are drawn in a 250×350 box (a 5:7 poker card) and scaled by the SVG, so every size stays crisp.
const W = 250;
const H = 350;

/** Suit shapes in a 100×100 box. Paths rather than glyphs, so no platform swaps in an emoji. */
const SUIT_PATHS: Record<Suit, React.ReactNode> = {
  hearts: (
    <path d="M50 96 C44 84 2 60 2 31 C2 14 14 4 28 4 C39 4 47 11 50 21 C53 11 61 4 72 4 C86 4 98 14 98 31 C98 60 56 84 50 96 Z" />
  ),
  diamonds: <path d="M50 2 Q68 28 88 50 Q68 72 50 98 Q32 72 12 50 Q32 28 50 2 Z" />,
  spades: (
    <path d="M50 3 C56 18 98 40 98 64 C98 78 87 86 75 86 C65 86 57 80 53 72 C54 82 58 90 67 97 L33 97 C42 90 46 82 47 72 C43 80 35 86 25 86 C13 86 2 78 2 64 C2 40 44 18 50 3 Z" />
  ),
  clubs: (
    <>
      <circle cx="50" cy="27" r="21" />
      <circle cx="26" cy="60" r="21" />
      <circle cx="74" cy="60" r="21" />
      <circle cx="50" cy="54" r="12" />
      <path d="M46 58 C46 80 41 90 32 97 L68 97 C59 90 54 80 54 58 Z" />
    </>
  ),
};

const SuitIcon: React.FC<{ suit: Suit; x: number; y: number; size: number; flip?: boolean }> = ({
  suit,
  x,
  y,
  size,
  flip = false,
}) => (
  <g transform={`translate(${x} ${y}) rotate(${flip ? 180 : 0}) scale(${size / 100}) translate(-50 -50)`}>
    {SUIT_PATHS[suit]}
  </g>
);

// Pip columns and rows of a standard deck; pips below the middle are printed upside down.
const L = 88;
const C = 125;
const R = 162;
const TOP = 86;
const BOTTOM = 264;
const MID = 175;
const ROW_2 = 145.3;
const ROW_3 = 204.7;

const PIPS: Partial<Record<Rank, [number, number][]>> = {
  '2': [[C, TOP], [C, BOTTOM]],
  '3': [[C, TOP], [C, MID], [C, BOTTOM]],
  '4': [[L, TOP], [R, TOP], [L, BOTTOM], [R, BOTTOM]],
  '5': [[L, TOP], [R, TOP], [C, MID], [L, BOTTOM], [R, BOTTOM]],
  '6': [[L, TOP], [R, TOP], [L, MID], [R, MID], [L, BOTTOM], [R, BOTTOM]],
  '7': [[L, TOP], [R, TOP], [C, (TOP + MID) / 2], [L, MID], [R, MID], [L, BOTTOM], [R, BOTTOM]],
  '8': [[L, TOP], [R, TOP], [C, (TOP + MID) / 2], [L, MID], [R, MID], [C, (MID + BOTTOM) / 2], [L, BOTTOM], [R, BOTTOM]],
  '9': [[L, TOP], [R, TOP], [L, ROW_2], [R, ROW_2], [C, MID], [L, ROW_3], [R, ROW_3], [L, BOTTOM], [R, BOTTOM]],
  '10': [
    [L, TOP], [R, TOP], [C, (TOP + ROW_2) / 2], [L, ROW_2], [R, ROW_2],
    [L, ROW_3], [R, ROW_3], [C, (ROW_3 + BOTTOM) / 2], [L, BOTTOM], [R, BOTTOM],
  ],
};

const FACE_RANKS: Rank[] = ['J', 'Q', 'K'];

/** Rank over suit in the top-left, and the same turned round in the bottom-right. */
const CornerIndex: React.FC<{ rank: Rank; suit: Suit; flip?: boolean }> = ({ rank, suit, flip = false }) => (
  <g transform={flip ? `rotate(180 ${W / 2} ${H / 2})` : undefined}>
    <text
      x="27"
      y="50"
      textAnchor="middle"
      fontSize="46"
      fontWeight="700"
      letterSpacing={rank === '10' ? -3 : 0}
      fontFamily="'Outfit', 'Plus Jakarta Sans', sans-serif"
    >
      {rank}
    </text>
    <SuitIcon suit={suit} x={27} y={76} size={28} />
  </g>
);

const CardFace: React.FC<{ rank: Rank; suit: Suit; compact: boolean }> = ({ rank, suit, compact }) => {
  // Too small for pips to read: a large rank and suit, the way mobile card games draw minis.
  if (compact) {
    return (
      <>
        <text
          x="30"
          y="118"
          fontSize="120"
          fontWeight="700"
          letterSpacing={rank === '10' ? -14 : 0}
          fontFamily="'Outfit', 'Plus Jakarta Sans', sans-serif"
        >
          {rank}
        </text>
        <SuitIcon suit={suit} x={170} y={265} size={120} />
      </>
    );
  }

  const pips = PIPS[rank];
  return (
    <>
      <CornerIndex rank={rank} suit={suit} />
      <CornerIndex rank={rank} suit={suit} flip />
      {rank === 'A' && <SuitIcon suit={suit} x={C} y={MID} size={suit === 'spades' ? 120 : 96} />}
      {pips?.map(([x, y], i) => <SuitIcon key={i} suit={suit} x={x} y={y} size={40} flip={y > MID} />)}
      {FACE_RANKS.includes(rank) && (
        <g>
          <rect x="58" y="44" width="134" height="262" rx="10" fill="currentColor" fillOpacity="0.07" />
          <rect x="58" y="44" width="134" height="262" rx="10" fill="none" stroke="currentColor" strokeWidth="2.5" />
          <rect x="66" y="52" width="118" height="246" rx="6" fill="none" stroke="currentColor" strokeOpacity="0.35" strokeWidth="1.5" />
          <SuitIcon suit={suit} x={86} y={76} size={30} />
          <SuitIcon suit={suit} x={164} y={274} size={30} flip />
          <text
            x={C}
            y={MID + 36}
            textAnchor="middle"
            fontSize="104"
            fontWeight="700"
            fontFamily="'Playfair Display', Georgia, 'Times New Roman', serif"
          >
            {rank}
          </text>
        </g>
      )}
    </>
  );
};

export const CardView: React.FC<CardViewProps> = ({
  card,
  faceDown = false,
  selected = false,
  disabled = false,
  onClick,
  size = 'md',
  fourColor = false,
  cardBack = 'geometric-blue',
  className = '',
  tooltip,
}) => {
  const sizeClasses = {
    // Explicit 5:7 heights, not aspect-ratio, so a stretching flex parent cannot distort the card.
    sm: 'w-10 h-14 rounded-[4px]',
    md: 'w-16 h-[5.6rem] sm:w-18 sm:h-[6.3rem] md:w-20 md:h-28 rounded-md',
    lg: 'w-20 h-28 sm:w-24 sm:h-[8.4rem] rounded-lg',
  }[size];

  if (faceDown || !card) {
    // Muted, print-like backs: ink lattice (default), table-felt crosshatch, oxblood diamonds.
    const backPattern = () => {
      if (cardBack === 'crimson-diamond') {
        return 'bg-[#6b1f26] bg-[radial-gradient(rgba(255,255,255,0.16)_1px,transparent_1px)] [background-size:6px_6px]';
      }
      if (cardBack === 'classic-cross') {
        return 'bg-felt-800 bg-[repeating-linear-gradient(45deg,rgba(255,255,255,0.09)_0,rgba(255,255,255,0.09)_1px,transparent_0,transparent_5px),repeating-linear-gradient(-45deg,rgba(255,255,255,0.09)_0,rgba(255,255,255,0.09)_1px,transparent_0,transparent_5px)]';
      }
      return 'bg-stone-800 bg-[repeating-linear-gradient(45deg,rgba(255,255,255,0.08)_0,rgba(255,255,255,0.08)_1px,transparent_0,transparent_6px),repeating-linear-gradient(-45deg,rgba(255,255,255,0.08)_0,rgba(255,255,255,0.08)_1px,transparent_0,transparent_6px)]';
    };

    // A white margin round the pattern, as on a printed card.
    return (
      <div
        className={`${sizeClasses} shrink-0 bg-white border border-stone-300 ${size === 'sm' ? 'p-0.5' : 'p-1'} shadow-[0_1px_3px_rgba(15,23,42,0.25)] select-none ${className}`}
        title={tooltip}
      >
        <div className={`w-full h-full rounded-[3px] border border-black/10 ${backPattern()}`} />
      </div>
    );
  }

  const getSuitColor = (suit: Suit): string => {
    if (fourColor) {
      switch (suit) {
        case 'hearts':
          return 'text-[#c8102e]';
        case 'spades':
          return 'text-stone-950';
        case 'diamonds':
          return 'text-[#1f5fa8]';
        case 'clubs':
          return 'text-emerald-700';
      }
    }
    return suit === 'hearts' || suit === 'diamonds' ? 'text-[#c8102e]' : 'text-stone-950';
  };

  const suitGlyphs: Record<Suit, string> = {
    hearts: '♥',
    spades: '♠',
    diamonds: '♦',
    clubs: '♣',
  };

  return (
    <div
      onClick={!disabled ? onClick : undefined}
      title={tooltip || `${card.rank}${suitGlyphs[card.suit]}`}
      className={`
        ${sizeClasses}
        relative shrink-0 overflow-hidden bg-white border select-none
        transition-[transform,box-shadow] duration-150 ease-out
        ${getSuitColor(card.suit)}
        ${
          selected
            ? '-translate-y-4 border-felt-500 ring-2 ring-felt-500 shadow-[0_10px_20px_rgba(15,23,42,0.3)]'
            : disabled
            ? 'cursor-not-allowed border-stone-300 shadow-[0_1px_2px_rgba(15,23,42,0.2)]'
            : `border-stone-300 shadow-[0_1px_3px_rgba(15,23,42,0.25)] ${
                onClick ? 'hover:-translate-y-2 hover:shadow-[0_8px_16px_rgba(15,23,42,0.25)] cursor-pointer' : ''
              }`
        }
        ${className}
      `}
    >
      <svg viewBox={`0 0 ${W} ${H}`} className="w-full h-full" fill="currentColor" aria-hidden="true">
        <CardFace rank={card.rank} suit={card.suit} compact={size === 'sm'} />
      </svg>
      {disabled && <div className="absolute inset-0 bg-stone-900/20" />}
    </div>
  );
};
