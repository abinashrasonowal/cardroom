import React from 'react';
import { GameDefinition } from '@/types/game';

interface GameCardProps {
  def: GameDefinition;
  selected: boolean;
  onSelect: () => void;
}

const chip = 'text-[10px] px-2 py-0.5 rounded font-medium';

/** One game in the lobby grid, rendered entirely from its GameDefinition. */
export const GameCard: React.FC<GameCardProps> = ({ def, selected, onSelect }) => {
  const redSuit = def.suitColor.includes('red');

  return (
    <div
      onClick={onSelect}
      className={`cursor-pointer rounded-2xl p-4 transition-all duration-150 relative shadow-sm flex flex-col justify-between min-h-[170px] ${
        selected
          ? 'border-2 border-blue-600 bg-blue-50/70'
          : 'border border-slate-200 hover:border-slate-400 bg-white hover:bg-slate-50'
      }`}
    >
      <div>
        <div className="flex items-start justify-between gap-2 mb-3">
          <div
            className={`w-11 h-11 rounded-xl border flex items-center justify-center text-2xl shadow-sm shrink-0 ${
              redSuit ? 'bg-white border-red-200' : 'bg-slate-50 border-slate-200'
            } ${def.suitColor}`}
          >
            {def.icon ?? def.suitGlyph}
          </div>
          <div className="flex items-center gap-1.5">
            {def.live && (
              <span className="text-[10px] bg-emerald-600 text-white font-bold px-2 py-0.5 rounded-full">
                Live
              </span>
            )}
            {selected && (
              <span className="text-[10px] bg-blue-600 text-white font-bold px-2 py-0.5 rounded-full">
                Active
              </span>
            )}
            {def.badge !== def.playerCountLabel && (
              <span className={`${chip} text-slate-700 bg-slate-100 border border-slate-200`}>{def.badge}</span>
            )}
            <span
              className={`${chip} ${
                selected ? 'text-blue-700 bg-blue-100 border border-blue-200' : 'text-slate-500 bg-slate-100'
              }`}
            >
              {def.playerCountLabel}
            </span>
          </div>
        </div>
        <h3 className="text-lg font-bold text-slate-950 leading-tight font-space">{def.name}</h3>
        <p className="text-xs sm:text-sm text-slate-600 mt-1 leading-snug">{def.description}</p>
      </div>
      <div
        className={`flex items-center justify-between pt-3 border-t mt-3 text-xs font-semibold ${
          selected ? 'border-blue-200 text-blue-600' : 'border-slate-200 text-slate-500'
        }`}
      >
        <span className="uppercase tracking-wider">{selected ? 'Configuring' : 'Select Game'}</span>
        <span className="material-symbols-outlined text-base">{selected ? 'check_circle' : 'arrow_forward'}</span>
      </div>
    </div>
  );
};
