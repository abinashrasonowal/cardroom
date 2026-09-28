import React, { useState } from 'react';
import { soundFx } from '@/utils/audio';

interface QuickJoinProps {
  onJoin: (roomCode: string, nickname: string) => void;
}

const label = 'text-[10px] text-slate-500 uppercase font-bold tracking-wide';
const field =
  'w-full bg-white border border-slate-300 focus:border-violet-600 focus:ring-1 focus:ring-violet-600 rounded-lg px-3 h-11 text-sm text-slate-900 placeholder:text-slate-400 outline-none transition-colors';

/** The top of the lobby: page headline on the left, join-by-code card on the right. */
export const QuickJoin: React.FC<QuickJoinProps> = ({ onJoin }) => {
  const [quickJoinName, setQuickJoinName] = useState<string>('');
  const [quickJoinCode, setQuickJoinCode] = useState<string>('');

  const handleQuickJoinSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    soundFx.playClick();
    if (!quickJoinCode.trim()) return;
    onJoin(quickJoinCode.trim().toUpperCase(), quickJoinName.trim() || 'Guest');
  };

  return (
    <section className="grid grid-cols-1 lg:grid-cols-3 gap-7 lg:gap-12 items-center py-2">
      {/* Page headline */}
      <div className="flex flex-col gap-3 lg:col-span-2">
        <h1 className="font-space text-3xl md:text-4xl font-bold text-slate-950 leading-[1.15]">
          Play Classic &amp; Modern Card Games with{' '}
          <span className="text-violet-600">Friends Online</span>
        </h1>
        <p className="text-sm text-slate-600 leading-relaxed max-w-lg">
          Jump straight into a live table, craft your own house rules, or punch in a room code to
          join your crew — no downloads and no signup.
        </p>
      </div>

      {/* Join by code */}
      <form
        onSubmit={handleQuickJoinSubmit}
        className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm flex flex-col gap-4"
      >
        <div className="flex items-center gap-2">
          <span className="material-symbols-outlined text-lg text-violet-600">vpn_key</span>
          <h2 className="font-space text-lg font-bold text-slate-950">Enter Room Code</h2>
        </div>

        <p className="text-xs text-slate-600 leading-relaxed -mt-1">
          Have a room code from your host? Type it in below with the name you want at the table.
        </p>

        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex flex-col gap-1.5 flex-1 min-w-0">
            <span className={label}>Your Nickname</span>
            <input
              type="text"
              value={quickJoinName}
              onChange={(e) => setQuickJoinName(e.target.value)}
              placeholder="e.g. Alex"
              className={field}
            />
          </div>

          <div className="flex flex-col gap-1.5 sm:w-44 shrink-0">
            <span className={label}>Room Code</span>
            <input
              type="text"
              maxLength={8}
              value={quickJoinCode}
              onChange={(e) => setQuickJoinCode(e.target.value.toUpperCase())}
              placeholder="HRT-8429"
              className={`${field} font-mono-code text-center tracking-[0.3em] font-bold uppercase text-violet-700`}
            />
          </div>
        </div>

        <button
          type="submit"
          className="h-11 rounded-lg bg-violet-600 hover:bg-violet-700 text-white text-sm font-bold flex items-center justify-center gap-2 transition-colors shadow-md shadow-violet-200 cursor-pointer"
        >
          <span>Join Room</span>
          <span className="material-symbols-outlined text-base">arrow_forward</span>
        </button>
      </form>
    </section>
  );
};
