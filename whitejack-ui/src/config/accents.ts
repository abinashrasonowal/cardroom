import { GameAccent } from '@/types/game';

/**
 * Per-game colour dressing for the lobby cards. Every class is written out in full because
 * Tailwind only sees class names that appear literally in the source.
 */
export interface AccentTheme {
  /** Card background gradient plus its resting border. */
  surface: string;
  /** Soft glow sitting behind the card fan. */
  glow: string;
  /** Player-count chip. */
  chip: string;
  /** The card's own action button when the game is not the selected one. */
  action: string;
}

export const ACCENTS: Record<GameAccent, AccentTheme> = {
  blue: {
    surface: 'bg-gradient-to-br from-sky-50 via-white to-blue-100/70 border-blue-100',
    glow: 'bg-blue-300/30',
    chip: 'bg-white/70 text-blue-700 border-blue-100',
    action: 'bg-white/80 text-blue-700 border-blue-100',
  },
  rose: {
    surface: 'bg-gradient-to-br from-rose-50 via-white to-pink-100/70 border-rose-100',
    glow: 'bg-rose-300/30',
    chip: 'bg-white/70 text-rose-700 border-rose-100',
    action: 'bg-white/80 text-rose-700 border-rose-100',
  },
  amber: {
    surface: 'bg-gradient-to-br from-amber-50 via-white to-yellow-100/70 border-amber-100',
    glow: 'bg-amber-300/30',
    chip: 'bg-white/70 text-amber-700 border-amber-100',
    action: 'bg-white/80 text-amber-700 border-amber-100',
  },
  emerald: {
    surface: 'bg-gradient-to-br from-emerald-50 via-white to-green-100/70 border-emerald-100',
    glow: 'bg-emerald-300/30',
    chip: 'bg-white/70 text-emerald-700 border-emerald-100',
    action: 'bg-white/80 text-emerald-700 border-emerald-100',
  },
  violet: {
    surface: 'bg-gradient-to-br from-violet-50 via-white to-indigo-100/70 border-violet-100',
    glow: 'bg-violet-300/30',
    chip: 'bg-white/70 text-violet-700 border-violet-100',
    action: 'bg-white/80 text-violet-700 border-violet-100',
  },
  orange: {
    surface: 'bg-gradient-to-br from-orange-50 via-white to-amber-100/70 border-orange-100',
    glow: 'bg-orange-300/30',
    chip: 'bg-white/70 text-orange-700 border-orange-100',
    action: 'bg-white/80 text-orange-700 border-orange-100',
  },
  slate: {
    surface: 'bg-gradient-to-br from-slate-50 via-white to-slate-200/70 border-slate-200',
    glow: 'bg-slate-400/25',
    chip: 'bg-white/70 text-slate-700 border-slate-200',
    action: 'bg-white/80 text-slate-700 border-slate-200',
  },
};
