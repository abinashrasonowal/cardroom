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
}

export const Header: React.FC<HeaderProps> = ({
  soundEnabled,
  onToggleSound,
  onOpenAbout,
  onOpenSettings,
  inGame = false,
  gameTitle,
  roomCode,
  onLeaveGame,
}) => {
  return (
    <header className="fixed top-0 left-0 w-full z-50 bg-white/95 backdrop-blur-xl border-b border-slate-200">
      <div className="h-20 w-full px-4 sm:px-8 flex items-center justify-between gap-4 max-w-[1560px] mx-auto">
        {/* Brand / Logo */}
        <div className="flex items-center gap-4">
          <button
            onClick={inGame ? onLeaveGame : undefined}
            className={`flex items-center gap-3 text-left transition-opacity ${
              inGame ? 'hover:opacity-85 cursor-pointer' : ''
            }`}
          >
            {/* Logo Badge */}
            <div className="h-9 w-9 rounded-lg bg-slate-50 border border-slate-200 flex items-center justify-center gap-0.5 shadow-sm shrink-0">
              <span className="text-blue-600 font-bold text-base leading-none">♠</span>
              <span className="text-red-600 font-bold text-base leading-none">♥</span>
            </div>
            <div className="flex flex-col">
              <span className="font-space text-lg sm:text-xl tracking-wider text-slate-950 uppercase font-bold leading-none">
                CARDROOM
              </span>
              <span className="text-[10px] sm:text-xs text-blue-600 font-semibold tracking-widest uppercase mt-0.5">
                Private Games with Friends
              </span>
            </div>
          </button>

          {/* In-Game Status indicator */}
          {inGame && roomCode && (
            <div className="hidden md:flex items-center gap-2 pl-4 border-l border-slate-200">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                {gameTitle} Table
              </span>
              <span className="font-mono-code font-bold text-xs bg-blue-50 text-blue-700 border border-blue-200 px-2 py-0.5 rounded">
                {roomCode}
              </span>
            </div>
          )}
        </div>

        {/* Actions Zone */}
        <div className="flex items-center gap-2 sm:gap-3">
          {inGame && (
            <button
              onClick={onLeaveGame}
              className="text-xs font-semibold text-slate-600 hover:text-slate-950 px-3 py-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 transition-colors flex items-center gap-1.5 cursor-pointer"
            >
              <span className="material-symbols-outlined text-base">arrow_back</span>
              <span className="hidden sm:inline">Back to Lobby</span>
            </button>
          )}

          <button
            onClick={onOpenAbout}
            className="text-xs font-semibold text-slate-600 hover:text-slate-950 px-2.5 sm:px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 cursor-pointer"
          >
            <span className="material-symbols-outlined text-lg">info</span>
            <span className="hidden sm:inline">About</span>
          </button>

          <div className="h-4 w-px bg-slate-200 mx-0.5 sm:mx-1"></div>

          <button
            onClick={onToggleSound}
            className="p-2 rounded-lg bg-slate-50 border border-slate-200 text-slate-700 hover:text-slate-950 hover:bg-slate-100 transition-colors shadow-sm cursor-pointer"
            title={soundEnabled ? 'Mute Sound Effects' : 'Unmute Sound Effects'}
          >
            <span className="material-symbols-outlined text-lg leading-none">
              {soundEnabled ? 'volume_up' : 'volume_off'}
            </span>
          </button>

          <button
            onClick={onOpenSettings}
            className="p-2 rounded-lg bg-slate-50 border border-slate-200 text-slate-700 hover:text-slate-950 hover:bg-slate-100 transition-colors shadow-sm cursor-pointer"
            title="Table Preferences"
          >
            <span className="material-symbols-outlined text-lg leading-none">settings</span>
          </button>
        </div>
      </div>
    </header>
  );
};
