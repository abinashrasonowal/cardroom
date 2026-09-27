import React from 'react';

interface FooterProps {
  soundEnabled: boolean;
  onToggleSound: () => void;
  onOpenSettings: () => void;
}

export const Footer: React.FC<FooterProps> = ({
  soundEnabled,
  onToggleSound,
  onOpenSettings,
}) => {
  return (
    <footer className="w-full bg-white border-t border-slate-200 text-slate-600">
      <div className="w-full px-4 sm:px-8 flex flex-wrap items-center justify-between gap-4 max-w-[1560px] mx-auto py-5">
        {/* Left: Telemetry & Quality Markers */}
        <div className="flex items-center gap-4 sm:gap-6 flex-wrap">
          <div className="flex items-center gap-2">
            <span className="relative flex h-2.5 w-2.5">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-500 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-blue-600"></span>
            </span>
            <span className="text-xs sm:text-sm text-slate-950 font-bold tabular-nums">
              14,289
            </span>
            <span className="text-xs text-slate-500 font-medium">Players Online</span>
          </div>

          <div className="hidden md:flex items-center gap-3 text-slate-500 text-xs">
            <span className="text-slate-600 font-medium">Pure Card Games</span>
            <span>·</span>
            <span>No Real Money</span>
            <span>·</span>
            <span>Private Multiplayer</span>
          </div>
        </div>

        {/* Right: Controls & Copyright */}
        <div className="flex items-center gap-4 sm:gap-6">
          <div className="flex items-center gap-1.5">
            <button
              onClick={onToggleSound}
              className="p-1.5 rounded-lg bg-slate-50 border border-slate-200 text-slate-600 hover:text-slate-950 hover:bg-slate-100 transition-colors shadow-sm cursor-pointer"
              title="Sound Effects"
            >
              <span className="material-symbols-outlined text-lg leading-none">
                {soundEnabled ? 'volume_up' : 'volume_off'}
              </span>
            </button>
            <button
              onClick={onOpenSettings}
              className="p-1.5 rounded-lg bg-slate-50 border border-slate-200 text-slate-600 hover:text-slate-950 hover:bg-slate-100 transition-colors shadow-sm cursor-pointer"
              title="Settings"
            >
              <span className="material-symbols-outlined text-lg leading-none">settings</span>
            </button>
          </div>

          <div className="text-xs text-slate-400">
            © 2024 Cardroom. Built for game nights.
          </div>
        </div>
      </div>
    </footer>
  );
};
