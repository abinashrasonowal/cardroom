import React, { useState } from 'react';
import { soundFx } from '@/utils/audio';

interface JoinInviteProps {
  room: string;
  /** Prefill: the name used last time on this browser, if any. */
  initialNickname: string;
  onJoin: (nickname: string) => void;
  onCancel: () => void;
}

/** Matches the server's limit in RoomActor. */
const MAX_NICK = 24;

/** Opened from an invite link: ask who is joining before taking a seat. */
export const JoinInvite: React.FC<JoinInviteProps> = ({ room, initialNickname, onJoin, onCancel }) => {
  const [nickname, setNickname] = useState(initialNickname);
  const trimmed = nickname.trim();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!trimmed) return;
    soundFx.playClick();
    onJoin(trimmed);
  };

  return (
    <div className="w-full max-w-sm mx-auto px-4 sm:px-6 pt-8 animate-rise">
      <form
        onSubmit={handleSubmit}
        aria-labelledby="invite-heading"
        className="bg-white border border-stone-200 rounded-xl p-6 shadow-raised flex flex-col gap-5"
      >
        <div className="flex flex-col gap-1.5">
          <span className="text-[13px] font-medium text-felt-700">You’ve been invited</span>
          <h1 id="invite-heading" className="font-display text-2xl font-semibold text-stone-900">
            Join room <code className="font-mono-code font-medium tracking-wider">{room}</code>
          </h1>
          <p className="text-[13px] text-stone-500">Choose the name the table will see.</p>
        </div>

        <label className="flex flex-col gap-1.5">
          <span className="text-[13px] font-medium text-stone-700">Your name</span>
          <input
            type="text"
            name="nickname"
            autoComplete="nickname"
            spellCheck={false}
            autoFocus
            maxLength={MAX_NICK}
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            placeholder="Alex…"
            className="bg-white border border-stone-300 hover:border-stone-400 focus:border-stone-900 focus:ring-2 focus:ring-stone-900/10 rounded-md px-3 h-10 text-sm text-stone-900 placeholder:text-stone-400 outline-none transition-[border-color,box-shadow]"
          />
        </label>

        <div className="flex flex-col gap-2">
          <button
            type="submit"
            disabled={!trimmed}
            className="h-10 rounded-md bg-stone-900 text-white text-sm font-medium hover:bg-stone-800 active:scale-[0.99] transition-[background-color,transform] disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
          >
            Take a seat
          </button>
          <button
            type="button"
            onClick={onCancel}
            className="h-10 rounded-md text-sm font-medium text-stone-600 hover:text-stone-900 hover:bg-stone-100 transition-colors cursor-pointer"
          >
            Back to lobby
          </button>
        </div>
      </form>
    </div>
  );
};
