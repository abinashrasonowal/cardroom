import React from 'react';

/** `wide` matches the in-game layout, which spans more of the screen than the lobby. */
export const Footer: React.FC<{ wide?: boolean }> = ({ wide = false }) => {
  return (
    <footer className="w-full border-t border-stone-200 text-stone-500">
      <div className={`w-full px-4 sm:px-6 flex flex-wrap items-center justify-between gap-x-6 gap-y-2 mx-auto py-5 text-[13px] ${wide ? 'max-w-[1440px]' : 'max-w-[1200px]'}`}>
        <p>Card games for friends. No accounts, no ads, no real money.</p>
        <div className="flex items-center gap-5">
          <a href="#/about" className="hover:text-stone-900 transition-colors">
            About
          </a>
          <p className="tabular">© {new Date().getFullYear()} Whitejack</p>
        </div>
      </div>
    </footer>
  );
};
