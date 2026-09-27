import React, { useState } from 'react';
import { soundFx } from '@/utils/audio';

interface QuickJoinProps {
  onJoin: (roomCode: string, nickname: string) => void;
}

/** "Have a Room Code?" — joins a live room (six-character code) or an offline demo table. */
export const QuickJoin: React.FC<QuickJoinProps> = ({ onJoin }) => {
  const [quickJoinName, setQuickJoinName] = useState<string>('Alex');
  const [quickJoinCode, setQuickJoinCode] = useState<string>('HRT-8429');

  const handleQuickJoinSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    soundFx.playClick();
    if (!quickJoinCode.trim()) return;
    onJoin(quickJoinCode.trim().toUpperCase(), quickJoinName.trim() || 'Guest');
  };

  return (
    <section className="w-full bg-slate-50 border border-slate-200 rounded-2xl p-6 md:p-7 shadow-sm flex flex-col xl:flex-row xl:items-center justify-between gap-6 relative overflow-hidden">
      <div className="absolute -right-16 -top-16 w-64 h-64 bg-blue-600/5 rounded-full blur-3xl pointer-events-none" />

      <div className="flex flex-col gap-2 max-w-xl relative z-10">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-white border border-slate-200 shadow-sm flex items-center justify-center text-blue-600">
            <span className="material-symbols-outlined text-lg">vpn_key</span>
          </div>
          <span className="text-xs text-blue-600 uppercase font-bold tracking-widest">
            Quick Join
          </span>
        </div>
        <h2 className="text-2xl sm:text-3xl text-slate-950 font-bold tracking-tight font-space">
          Have a Room Code?
        </h2>
        <p className="text-xs sm:text-sm text-slate-600">
          Enter an active room code or paste your invite link to jump directly into your friends'
          game table.
        </p>
      </div>

      <form
        onSubmit={handleQuickJoinSubmit}
        className="flex flex-wrap sm:flex-nowrap items-center gap-3 w-full xl:w-auto xl:min-w-[520px] justify-end relative z-10"
      >
        <div className="flex flex-col gap-1.5 w-full sm:w-auto flex-1 min-w-[150px]">
          <span className="text-xs text-slate-700 uppercase font-semibold">Your Nickname</span>
          <input
            type="text"
            value={quickJoinName}
            onChange={(e) => setQuickJoinName(e.target.value)}
            placeholder="e.g. Alex"
            className="bg-white border border-slate-300 focus:border-blue-600 focus:ring-1 focus:ring-blue-600 rounded-lg px-3.5 h-11 text-sm text-slate-900 placeholder:text-slate-400 w-full transition-colors shadow-sm"
          />
        </div>

        <div className="flex flex-col gap-1.5 w-full sm:w-auto shrink-0">
          <span className="text-xs text-slate-700 uppercase font-semibold">Room Code</span>
          <input
            type="text"
            maxLength={8}
            value={quickJoinCode}
            onChange={(e) => setQuickJoinCode(e.target.value.toUpperCase())}
            placeholder="HRT-8429"
            className="bg-white border border-slate-300 focus:border-blue-600 focus:ring-1 focus:ring-blue-600 rounded-lg px-3.5 h-11 font-mono-code text-center text-blue-600 uppercase tracking-wider placeholder:text-slate-400 text-base font-bold w-full sm:w-36 shrink-0 transition-colors shadow-sm"
          />
        </div>

        <div className="flex flex-col gap-1.5 w-full sm:w-auto shrink-0">
          <span className="text-xs text-transparent select-none hidden sm:inline-block">Join</span>
          <button
            type="submit"
            className="h-11 px-6 rounded-lg bg-blue-600 text-white hover:bg-blue-700 text-xs font-bold uppercase tracking-wider border border-blue-600 flex items-center justify-center gap-2 transition-all duration-150 shadow-sm shrink-0 w-full sm:w-auto cursor-pointer"
          >
            <span>Join Room</span>
            <span className="material-symbols-outlined text-base">arrow_forward</span>
          </button>
        </div>
      </form>
    </section>
  );
};
