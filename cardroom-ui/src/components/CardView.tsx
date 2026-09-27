import React from 'react';
import { Card, Suit } from '@/types/game';

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
  // Size dimensions
  const sizeClasses = {
    sm: 'w-10 h-14 text-xs rounded',
    md: 'w-16 h-23 sm:w-18 sm:h-26 md:w-20 md:h-28 text-sm rounded-lg',
    lg: 'w-20 h-28 sm:w-24 sm:h-34 text-base rounded-xl',
  }[size];

  if (faceDown || !card) {
    // Render Card Back
    const backPattern = () => {
      if (cardBack === 'crimson-diamond') {
        return 'bg-red-700 bg-[radial-gradient(#ffffff_1px,transparent_1px)] [background-size:6px_6px] border-red-800';
      }
      if (cardBack === 'classic-cross') {
        return 'bg-blue-900 bg-[repeating-linear-gradient(45deg,#1d4ed8_0,#1d4ed8_1px,transparent_0,transparent_5px)] border-blue-950';
      }
      // Geometric Blue
      return 'bg-blue-600 bg-[radial-gradient(circle_at_center,rgba(255,255,255,0.2)_1px,transparent_1px)] [background-size:8px_8px] border-blue-700';
    };

    return (
      <div
        className={`${sizeClasses} border-2 relative flex items-center justify-center p-1.5 shadow-sm transition-all select-none ${backPattern()} ${className}`}
        title={tooltip}
      >
        <div className="w-full h-full rounded border border-white/30 flex items-center justify-center">
          <div className="w-3.5 h-3.5 rounded-full border border-white/40 flex items-center justify-center text-[10px] text-white/80 font-bold">
            ♠
          </div>
        </div>
      </div>
    );
  }

  // Determine color based on suit & 4-color mode
  const getSuitColor = (suit: Suit): string => {
    if (fourColor) {
      switch (suit) {
        case 'hearts':
          return 'text-red-600';
        case 'spades':
          return 'text-slate-950';
        case 'diamonds':
          return 'text-blue-600';
        case 'clubs':
          return 'text-emerald-700';
      }
    }
    // Standard 2-color
    return suit === 'hearts' || suit === 'diamonds' ? 'text-red-600' : 'text-slate-950';
  };

  const suitGlyphs: Record<Suit, string> = {
    hearts: '♥',
    spades: '♠',
    diamonds: '♦',
    clubs: '♣',
  };

  const suitColorClass = getSuitColor(card.suit);

  return (
    <div
      onClick={!disabled ? onClick : undefined}
      title={tooltip || `${card.rank}${suitGlyphs[card.suit]}`}
      className={`
        ${sizeClasses}
        relative bg-white border flex flex-col justify-between p-1.5 sm:p-2 select-none
        transition-all duration-150
        ${
          selected
            ? 'ring-2 ring-blue-600 -translate-y-3.5 shadow-lg border-blue-600'
            : disabled
            ? 'opacity-40 cursor-not-allowed border-slate-200'
            : 'border-slate-200 shadow-sm hover:-translate-y-2 hover:shadow-md hover:border-slate-300 cursor-pointer active:translate-y-0'
        }
        ${className}
      `}
    >
      {/* Top Left Corner */}
      <div className={`flex flex-col items-center leading-none ${suitColorClass}`}>
        <span className="font-bold tracking-tight font-space text-xs sm:text-sm">
          {card.rank}
        </span>
        <span className="text-[11px] sm:text-xs leading-none">
          {suitGlyphs[card.suit]}
        </span>
      </div>

      {/* Center Suit Art */}
      <div className={`self-center text-xl sm:text-2xl md:text-3xl leading-none ${suitColorClass}`}>
        {suitGlyphs[card.suit]}
      </div>

      {/* Bottom Right Corner (Inverted) */}
      <div className={`flex flex-col items-center leading-none rotate-180 ${suitColorClass}`}>
        <span className="font-bold tracking-tight font-space text-xs sm:text-sm">
          {card.rank}
        </span>
        <span className="text-[11px] sm:text-xs leading-none">
          {suitGlyphs[card.suit]}
        </span>
      </div>
    </div>
  );
};
