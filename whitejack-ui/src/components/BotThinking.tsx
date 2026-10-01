import React from 'react';

/** Shown on a bot's seat while it holds the turn. Says only that it is deciding — never what. */
export const BotThinking: React.FC = () => (
  <span className="flex items-center gap-1 text-[11px] font-normal text-felt-200" role="status">
    <span aria-hidden className="flex gap-0.5">
      {[0, 1, 2].map((i) => (
        <span
          key={i}
          className="h-1 w-1 rounded-full bg-felt-300 animate-pulse"
          style={{ animationDelay: `${i * 160}ms` }}
        />
      ))}
    </span>
    thinking
  </span>
);
