import React from 'react';

export const Footer: React.FC = () => {
  return (
    <footer className="w-full bg-white/70 border-t border-slate-200 text-slate-600">
      <div className="w-full px-4 sm:px-8 flex flex-wrap items-center justify-between gap-4 max-w-[1560px] mx-auto py-5">
        {/* Left: Telemetry & Quality Markers */}
        <div className="flex items-center gap-4 sm:gap-6 flex-wrap">
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
          <div className="text-xs text-slate-400">
            © 2024 Whitejack. Built for game nights.
          </div>
        </div>
      </div>
    </footer>
  );
};
