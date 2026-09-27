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
    <div className="w-full max-w-md mx-auto px-4 sm:px-8">
      <form
        onSubmit={handleSubmit}
        className="bg-slate-50 border border-slate-200 rounded-2xl p-6 md:p-7 shadow-sm flex flex-col gap-5"
      >
        <div className="flex flex-col gap-1.5">
          <span className="text-xs text-blue-600 uppercase font-bold tracking-widest">You're invited</span>
          <h1 className="text-2xl text-slate-950 font-bold tracking-tight font-space">Join room</h1>
          <code className="text-xl font-mono-code font-bold tracking-wider text-blue-600">{room}</code>
        </div>

        <label className="flex flex-col gap-1.5">
          <span className="text-xs text-slate-700 uppercase font-semibold">Your nickname</span>
          <input
            type="text"
            autoFocus
            maxLength={MAX_NICK}
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            placeholder="e.g. Alex"
            className="bg-white border border-slate-300 focus:border-blue-600 focus:ring-1 focus:ring-blue-600 rounded-lg px-3.5 h-11 text-sm text-slate-900 placeholder:text-slate-400 shadow-sm"
          />
        </label>

        <div className="flex items-center gap-3">
          <button
            type="submit"
            disabled={!trimmed}
            className="h-11 px-6 rounded-lg bg-blue-600 text-white text-xs font-bold uppercase tracking-wider hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
          >
            Join room
          </button>
          <button
            type="button"
            onClick={onCancel}
            className="h-11 px-4 rounded-lg text-xs font-bold uppercase tracking-wider text-slate-600 hover:text-slate-900 cursor-pointer"
          >
            Back to lobby
          </button>
        </div>
      </form>
    </div>
  );
};
