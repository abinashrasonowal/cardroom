import React from 'react';

interface HeaderProps {
  soundEnabled: boolean;
  onToggleSound: () => void;
  onOpenAbout: () => void;
  onOpenSettings: () => void;
  inGame?: boolean;
  gameTitle?: string;
  roomCode?: string;
  onLeaveGame?: () => void;
  /** A live room fills the header's room slot itself, so the static chip and back button step aside. */
  liveRoom?: boolean;
}

/** LiveTable portals its room controls (code, invite, status, leave) into this element. */
export const ROOM_SLOT_ID = 'header-room-slot';

const roundButton =
  'h-10 w-10 rounded-full bg-white border border-slate-200 text-slate-600 hover:text-violet-700 hover:border-violet-200 transition-colors shadow-sm cursor-pointer flex items-center justify-center shrink-0';

export const Header: React.FC<HeaderProps> = ({
  soundEnabled,
  onToggleSound,
  onOpenAbout,
  onOpenSettings,
  inGame = false,
  gameTitle,
  roomCode,
  onLeaveGame,
  liveRoom = false,
}) => {
  return (
    <header className="fixed top-0 left-0 w-full z-50 bg-white/90 backdrop-blur-xl border-b border-slate-200/80">
      <div className="h-20 w-full px-4 sm:px-8 flex items-center justify-between gap-4 max-w-[1560px] mx-auto">
        {/* Brand / Logo */}
        <div className="flex items-center gap-4 shrink-0">
          <button
            onClick={inGame ? onLeaveGame : undefined}
            className={`flex items-center gap-3 text-left transition-opacity ${
              inGame ? 'hover:opacity-85 cursor-pointer' : ''
            }`}
          >
            {/* Logo Badge */}
            <div className="h-11 w-11 rounded-xl bg-white border border-slate-200 flex items-center justify-center gap-0.5 shadow-sm shrink-0">
              <span className="text-slate-900 font-bold text-lg leading-none">♠</span>
              <span className="text-red-500 font-bold text-lg leading-none">♥</span>
            </div>
            {/* In a live room a phone needs the width for the room controls; the logo alone stays. */}
            <div className={`flex flex-col ${liveRoom ? 'max-sm:hidden' : ''}`}>
              <span className="font-space text-lg sm:text-2xl tracking-tight text-slate-950 uppercase font-bold leading-none">
                WHITEJACK
              </span>
              <span className="text-[10px] sm:text-xs text-violet-600 font-semibold tracking-[0.18em] uppercase mt-1">
                Private Games with Friends
              </span>
            </div>
          </button>

          {/* In-Game Status indicator */}
          {inGame && roomCode && !liveRoom && (
            <div className="hidden md:flex items-center gap-2 pl-4 border-l border-slate-200">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                {gameTitle} Table
              </span>
              <span className="font-mono-code font-bold text-xs bg-violet-50 text-violet-700 border border-violet-100 px-2 py-0.5 rounded">
                {roomCode}
              </span>
            </div>
          )}
        </div>

        {/* Actions Zone */}
        <div className="flex items-center gap-2 sm:gap-2.5 shrink-0">
          {liveRoom && <div id={ROOM_SLOT_ID} className="flex items-center" />}
          {inGame && !liveRoom && (
            <button
              onClick={onLeaveGame}
              className="text-xs font-semibold text-slate-600 hover:text-slate-950 px-3 py-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 transition-colors flex items-center gap-1.5 cursor-pointer"
            >
              <span className="material-symbols-outlined text-base">arrow_back</span>
              <span className="hidden sm:inline">Back to Lobby</span>
            </button>
          )}

          <button
            onClick={onToggleSound}
            className={roundButton}
            title={soundEnabled ? 'Mute Sound Effects' : 'Unmute Sound Effects'}
          >
            <span className="material-symbols-outlined text-xl leading-none">
              {soundEnabled ? 'volume_up' : 'volume_off'}
            </span>
          </button>

          <button onClick={onOpenSettings} className={roundButton} title="Table Preferences">
            <span className="material-symbols-outlined text-xl leading-none">settings</span>
          </button>

          <button
            onClick={onOpenAbout}
            className={`${roundButton} ${liveRoom ? 'max-sm:hidden' : ''}`}
            title="About Whitejack"
          >
            <span className="material-symbols-outlined text-xl leading-none">info</span>
          </button>
        </div>
      </div>
    </header>
  );
};
