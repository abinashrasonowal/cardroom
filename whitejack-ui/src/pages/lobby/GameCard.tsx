import React from 'react';
import { CardFan, suitFromGlyph } from '@/components/CardFan';
import { GameDefinition } from '@/types/game';

interface GameCardProps {
  def: GameDefinition;
  /** Opens the configure page for this game. */
  onOpen: () => void;
  /** Position in the grid, used to stagger the entry animation. */
  index?: number;
}

/** One game in the lobby grid, rendered entirely from its GameDefinition. */
export const GameCard: React.FC<GameCardProps> = ({ def, onOpen, index = 0 }) => {
  const disabled = !!def.comingSoon;

  return (
    <button
      type="button"
      onClick={onOpen}
      disabled={disabled}
      style={{ animationDelay: `${80 + index * 60}ms` }}
      className={`group animate-rise text-left w-full bg-white border border-stone-200 rounded-xl p-4 flex gap-4 items-center transition-[border-color,box-shadow,transform] duration-200 ${
        disabled
          ? 'cursor-not-allowed opacity-60'
          : 'cursor-pointer hover:border-stone-300 hover:shadow-raised active:scale-[0.995]'
      }`}
    >
      <div className="shrink-0 rounded-lg bg-stone-100 w-20 h-20 flex items-center justify-center transition-colors group-hover:bg-felt-50">
        <CardFan suit={suitFromGlyph(def.suitGlyph)} size="sm" />
      </div>

      <div className="flex-1 min-w-0 flex flex-col gap-1">
        <div className="flex items-center justify-between gap-3">
          <h3 className="font-display text-lg font-semibold text-stone-900">{def.name}</h3>
          <span
            aria-hidden
            className="material-symbols-outlined text-[20px] text-stone-400 transition-[color,transform] duration-200 group-hover:text-stone-900 group-hover:translate-x-0.5"
          >
            {disabled ? 'schedule' : 'arrow_forward'}
          </span>
        </div>
        <p className="text-[13px] text-stone-600 leading-relaxed line-clamp-2">{def.description}</p>
        <div className="flex items-center gap-3 text-xs text-stone-500">
          <span className="flex items-center gap-1">
            <span aria-hidden className="material-symbols-outlined text-[15px]">group</span>
            {def.playerCountLabel}
          </span>
          {disabled && <span>Coming soon</span>}
        </div>
      </div>
    </button>
  );
};
