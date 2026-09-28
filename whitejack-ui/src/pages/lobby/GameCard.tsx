import React from 'react';
import { CardFan, suitFromGlyph } from '@/components/CardFan';
import { ACCENTS } from '@/config/accents';
import { GameDefinition } from '@/types/game';

interface GameCardProps {
  def: GameDefinition;
  /** Opens the configure page for this game. */
  onOpen: () => void;
}

const chip = 'text-[9px] px-1.5 py-0.5 rounded-full font-semibold border';

/** One game in the lobby grid, rendered entirely from its GameDefinition. */
export const GameCard: React.FC<GameCardProps> = ({ def, onOpen }) => {
  const accent = ACCENTS[def.accent];
  const disabled = !!def.comingSoon;

  return (
    <div
      onClick={disabled ? undefined : onOpen}
      aria-disabled={disabled}
      className={`group rounded-xl p-3.5 transition-all duration-150 border flex flex-col min-h-[168px] ${
        disabled ? 'opacity-60 cursor-not-allowed' : 'cursor-pointer hover:shadow-md hover:-translate-y-0.5'
      } ${accent.surface}`}
    >
      <div className="flex gap-2.5 flex-1">
        <div className="relative flex items-center">
          <span className={`absolute inset-0 m-auto w-12 h-12 rounded-full blur-2xl ${accent.glow}`} />
          <CardFan suit={suitFromGlyph(def.suitGlyph)} size="sm" className="relative" />
        </div>

        <div className="flex-1 min-w-0 flex flex-col">
          <div className="flex items-center flex-wrap gap-1 mb-1.5">
            {def.comingSoon && (
              <span className="text-[9px] bg-slate-700 text-white font-bold px-1.5 py-0.5 rounded-full border border-slate-700">
                Coming Soon
              </span>
            )}
            {def.badge !== def.playerCountLabel && (
              <span className={`${chip} ${accent.chip}`}>{def.badge}</span>
            )}
            <span className={`${chip} ${accent.chip}`}>{def.playerCountLabel}</span>
          </div>

          <h3 className="text-base font-bold text-slate-950 leading-tight font-space">{def.name}</h3>
          <p className="text-[11px] text-slate-600 mt-1 leading-relaxed">{def.description}</p>
        </div>
      </div>

      <button
        type="button"
        disabled={disabled}
        onClick={(e) => {
          // The card behind the button opens the same page, so stop the duplicate handler.
          e.stopPropagation();
          onOpen();
        }}
        className={`mt-3 w-full h-9 rounded-lg text-[11px] font-bold flex items-center justify-between px-3 border transition-colors ${
          disabled
            ? 'cursor-not-allowed'
            : 'cursor-pointer group-hover:bg-violet-600 group-hover:border-violet-600 group-hover:text-white'
        } ${accent.action}`}
      >
        <span>{disabled ? 'Coming Soon' : 'Play Now'}</span>
        <span className="material-symbols-outlined text-sm">{disabled ? 'schedule' : 'arrow_forward'}</span>
      </button>
    </div>
  );
};
