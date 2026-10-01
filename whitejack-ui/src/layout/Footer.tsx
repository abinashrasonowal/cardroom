import React from 'react';
import { GitHubMark } from '@/components/GitHubMark';
import { AUTHOR_NAME, AUTHOR_URL, GITHUB_URL, feedbackHref } from '@/config/site';

const link = 'hover:text-stone-900 transition-colors';

/** `wide` matches the in-game layout, which spans more of the screen than the lobby. */
export const Footer: React.FC<{ wide?: boolean; room?: string }> = ({ wide = false, room }) => {
  return (
    <footer className="w-full border-t border-stone-200 text-stone-500">
      <div
        className={`w-full px-4 sm:px-6 flex flex-wrap items-center justify-between gap-x-6 gap-y-3 mx-auto py-5 text-[13px] ${
          wide ? 'max-w-[1440px]' : 'max-w-[1200px]'
        }`}
      >
        <p>
          Built by{' '}
          <a href={AUTHOR_URL} target="_blank" rel="noreferrer" className={`${link} text-stone-700 font-medium`}>
            {AUTHOR_NAME}
          </a>
          . No accounts, no ads, no real money.
        </p>
        <nav aria-label="Footer" className="flex flex-wrap items-center gap-x-5 gap-y-2">
          <a href="#/about" className={link}>
            About
          </a>
          <a href={feedbackHref(room)} className={link}>
            Send feedback
          </a>
          <a href={GITHUB_URL} target="_blank" rel="noreferrer" className={`${link} flex items-center gap-1.5`}>
            <GitHubMark className="h-3.5 w-3.5" />
            Source
          </a>
        </nav>
      </div>
    </footer>
  );
};
