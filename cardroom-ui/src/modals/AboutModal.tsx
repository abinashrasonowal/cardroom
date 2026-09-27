import React from 'react';

interface AboutModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const AboutModal: React.FC<AboutModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white border border-slate-200 rounded-2xl max-w-lg w-full p-6 shadow-2xl flex flex-col gap-4 animate-scale-up">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-200 pb-3">
          <div className="flex items-center gap-2.5">
            <div className="h-8 w-8 rounded-lg bg-slate-50 border border-slate-200 flex items-center justify-center gap-0.5 shadow-sm">
              <span className="text-blue-600 font-bold text-sm leading-none">♠</span>
              <span className="text-red-600 font-bold text-sm leading-none">♥</span>
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-950 font-space uppercase">
                About Cardroom
              </h3>
              <p className="text-[11px] text-blue-600 font-semibold tracking-wider uppercase">
                Private Games with Friends
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-lg text-slate-400 hover:text-slate-800 hover:bg-slate-100 flex items-center justify-center text-lg transition-colors cursor-pointer"
          >
            ✕
          </button>
        </div>

        {/* Content */}
        <div className="flex flex-col gap-3 text-xs sm:text-sm text-slate-600 leading-relaxed">
          <p>
            <strong className="text-slate-900">Cardroom</strong> is a distraction-free, zero-signup platform designed specifically for private tabletop card games with friends and family.
          </p>

          <div className="bg-slate-50 border border-slate-200 rounded-xl p-3 flex flex-col gap-2">
            <span className="text-xs font-bold text-slate-900 uppercase tracking-wide">
              Core Principles
            </span>
            <ul className="list-disc list-inside space-y-1 text-xs text-slate-600">
              <li>
                <strong className="text-slate-800">Zero Signup:</strong> Pick a nickname, copy your room code, and deal. No passwords or accounts required.
              </li>
              <li>
                <strong className="text-slate-800">Pure Card Games:</strong> No ads, no casino loot boxes, no real-money gambling. Just classic trick-taking games.
              </li>
              <li>
                <strong className="text-slate-800">House Rule Customization:</strong> Toggle Omnibus Jack ♦, Blind Nil, stick the dealer, or customize decks in freeform sandbox mode.
              </li>
              <li>
                <strong className="text-slate-800">Tournament Legibility:</strong> Crisp card rendering, high-contrast suits, and responsive table animations.
              </li>
            </ul>
          </div>

          <p className="text-xs text-slate-500">
            Currently featuring Hearts, Spades (2v2), Euchre, Oh Hell, and a freeform Custom Deck Sandbox for homebrew rules.
          </p>
        </div>

        {/* Footer */}
        <div className="pt-2 border-t border-slate-200 flex justify-end">
          <button
            onClick={onClose}
            className="px-5 py-2 rounded-xl bg-blue-600 text-white text-xs font-bold uppercase tracking-wider hover:bg-blue-700 transition-colors shadow-sm cursor-pointer"
          >
            Got It
          </button>
        </div>
      </div>
    </div>
  );
};
