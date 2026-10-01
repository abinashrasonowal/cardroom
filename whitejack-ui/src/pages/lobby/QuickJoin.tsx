import React, { useState } from 'react';
import { soundFx } from '@/utils/audio';

interface QuickJoinProps {
  onJoin: (roomCode: string, nickname: string) => void;
}

const label = 'text-[13px] font-medium text-stone-700';
const field =
  'w-full bg-white border border-stone-300 hover:border-stone-400 focus:border-stone-900 focus:ring-2 focus:ring-stone-900/10 rounded-md px-3 h-10 text-sm text-stone-900 placeholder:text-stone-400 outline-none transition-[border-color,box-shadow]';

/** The top of the lobby: page headline on the left, join-by-code form on the right. */
export const QuickJoin: React.FC<QuickJoinProps> = ({ onJoin }) => {
  const [quickJoinName, setQuickJoinName] = useState<string>('');
  const [quickJoinCode, setQuickJoinCode] = useState<string>('');
  const [missingCode, setMissingCode] = useState<boolean>(false);

  const handleQuickJoinSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    soundFx.playClick();
    if (!quickJoinCode.trim()) {
      setMissingCode(true);
      document.getElementById('join-code')?.focus();
      return;
    }
    onJoin(quickJoinCode.trim().toUpperCase(), quickJoinName.trim() || 'Guest');
  };

  return (
    <section className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-16 items-center animate-rise">
      <div className="lg:col-span-7 flex flex-col gap-4">
        <h1 className="font-display text-[2.5rem] sm:text-5xl lg:text-[3.5rem] font-semibold text-stone-900 leading-[1.05]">
          A private card table for you and your friends.
        </h1>
        <p className="text-base text-stone-600 leading-relaxed max-w-[52ch]">
          Pick a game, share the room link and deal. No downloads, no accounts — fill empty seats
          with bots when the group is short.
        </p>
      </div>

      <form
        onSubmit={handleQuickJoinSubmit}
        noValidate
        aria-labelledby="join-heading"
        className="lg:col-span-5 bg-white border border-stone-200 rounded-xl p-5 sm:p-6 shadow-raised flex flex-col gap-4"
      >
        <div className="flex flex-col gap-1">
          <h2 id="join-heading" className="font-display text-base font-semibold text-stone-900">
            Join a room
          </h2>
          <p className="text-[13px] text-stone-500">Got a code from your host? Enter it here.</p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-[1fr_10rem] gap-3">
          <label className="flex flex-col gap-1.5 min-w-0">
            <span className={label}>Name</span>
            <input
              type="text"
              name="nickname"
              autoComplete="nickname"
              spellCheck={false}
              maxLength={24}
              value={quickJoinName}
              onChange={(e) => setQuickJoinName(e.target.value)}
              placeholder="Alex…"
              className={field}
            />
          </label>

          <label className="flex flex-col gap-1.5">
            <span className={label}>Room code</span>
            <input
              id="join-code"
              type="text"
              name="room-code"
              autoComplete="off"
              autoCapitalize="characters"
              spellCheck={false}
              maxLength={8}
              value={quickJoinCode}
              aria-invalid={missingCode}
              aria-describedby={missingCode ? 'join-code-error' : undefined}
              onChange={(e) => {
                setQuickJoinCode(e.target.value.toUpperCase());
                setMissingCode(false);
              }}
              placeholder="K7M2QX"
              className={`${field} font-mono-code tracking-[0.18em] uppercase ${
                missingCode ? 'border-red-500 focus:border-red-600 focus:ring-red-600/10' : ''
              }`}
            />
          </label>
        </div>

        {missingCode && (
          <p id="join-code-error" role="alert" className="text-[13px] text-red-700 -mt-1">
            Enter the room code your host shared.
          </p>
        )}

        <button
          type="submit"
          className="h-10 rounded-md bg-stone-900 hover:bg-stone-800 active:scale-[0.99] text-white text-sm font-medium flex items-center justify-center gap-1.5 transition-[background-color,transform] cursor-pointer"
        >
          Join room
          <span aria-hidden className="material-symbols-outlined text-[18px]">arrow_forward</span>
        </button>
      </form>
    </section>
  );
};
