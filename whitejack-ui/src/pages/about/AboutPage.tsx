import React from 'react';
import { CardFan, suitFromGlyph } from '@/components/CardFan';
import { GAME_DEFINITIONS } from '@/config/games';
import { GameKey } from '@/types/game';

/** The URL hash that opens this page. Room codes never contain a slash, so the two cannot collide. */
export const ABOUT_HASH = '#/about';

const GAMES: GameKey[] = ['poker', 'hearts', 'gin_rummy', 'spades'];

const STEPS = [
  {
    title: 'Open a table',
    body: 'Choose a game and enter the name the table will see. The server opens a private room with a six-character code.',
  },
  {
    title: 'Send the link',
    body: 'Share the invite link in whatever chat your group already uses. Friends pick a name and take a seat — no account, no download.',
  },
  {
    title: 'Deal',
    body: 'When enough players are seated the host starts the game. Short a player? Add a bot to the empty seat and play anyway.',
  },
];

const PRINCIPLES = [
  ['No accounts', 'A name and a room code are all it takes. Your identity lives in a signed browser cookie, not a profile.'],
  ['No money', 'Poker chips are for keeping score. There is nothing to buy, deposit or cash out.'],
  ['No ads', 'Nothing between you and the cards — no banners, no pop-ups, no loot boxes.'],
  ['Fair by construction', 'The server holds the deck and enforces every rule. Other players’ cards never reach your browser until they are shown.'],
] as const;

const sectionGrid = 'grid grid-cols-1 lg:grid-cols-12 gap-x-16 gap-y-8 py-14 sm:py-20 border-t border-stone-200';
const sectionHeading = 'font-display text-2xl font-semibold text-stone-900 leading-tight';

export const AboutPage: React.FC = () => {
  return (
    <article className="w-full max-w-[1200px] mx-auto px-4 sm:px-6 animate-rise">
      {/* Intro */}
      <header className="pt-6 pb-14 sm:pb-20 grid grid-cols-1 lg:grid-cols-12 gap-x-16 gap-y-6">
        <p className="lg:col-span-12 text-[13px] font-medium text-felt-700">About Whitejack</p>
        <h1 className="lg:col-span-8 font-display text-[2.5rem] sm:text-5xl lg:text-[3.5rem] font-semibold text-stone-900 leading-[1.05]">
          Built for the kitchen table, not the casino.
        </h1>
        <div className="lg:col-span-7 flex flex-col gap-4 text-base text-stone-600 leading-relaxed max-w-[62ch]">
          <p>
            Whitejack is a small, private place to play classic card games with people you already know. One
            person opens a table, everyone else joins from a link, and the game runs in the browser on any
            device.
          </p>
          <p>
            It started from a simple frustration: most online card sites want an account, a download, an ad
            impression or a deposit before you see a single card. This one only wants a name.
          </p>
        </div>
      </header>

      {/* How it works */}
      <section aria-labelledby="how-heading" className={sectionGrid}>
        <div className="lg:col-span-4">
          <h2 id="how-heading" className={`${sectionHeading} lg:sticky lg:top-24`}>
            How a game night works
          </h2>
        </div>
        <ol className="lg:col-span-8 flex flex-col divide-y divide-stone-200 border-t border-stone-200">
          {STEPS.map((step, i) => (
            <li key={step.title} className="grid grid-cols-[3rem_1fr] gap-4 py-6">
              <span className="font-mono-code text-sm text-stone-400 pt-0.5">{String(i + 1).padStart(2, '0')}</span>
              <div className="flex flex-col gap-1.5">
                <h3 className="font-display text-lg font-semibold text-stone-900">{step.title}</h3>
                <p className="text-[15px] text-stone-600 leading-relaxed max-w-[58ch]">{step.body}</p>
              </div>
            </li>
          ))}
        </ol>
      </section>

      {/* Games */}
      <section aria-labelledby="games-heading" className={sectionGrid}>
        <div className="lg:col-span-4 flex flex-col gap-3">
          <h2 id="games-heading" className={sectionHeading}>
            The games
          </h2>
          <p className="text-[15px] text-stone-600 leading-relaxed">
            Three games run online today, each with its full rules enforced by the server. More are on the way.
          </p>
        </div>
        <ul className="lg:col-span-8 grid grid-cols-1 sm:grid-cols-2 gap-3">
          {GAMES.map((key) => {
            const def = GAME_DEFINITIONS[key];
            return (
              <li key={key} className="bg-white border border-stone-200 rounded-xl p-5 flex flex-col gap-4">
                <div className="flex items-center justify-between gap-3">
                  <div className="rounded-lg bg-stone-100 w-16 h-16 flex items-center justify-center">
                    <div className="scale-75">
                      <CardFan suit={suitFromGlyph(def.suitGlyph)} size="sm" />
                    </div>
                  </div>
                  {def.comingSoon && <span className="text-xs text-stone-500">Coming soon</span>}
                </div>
                <div className="flex flex-col gap-1">
                  <h3 className="font-display text-lg font-semibold text-stone-900">{def.name}</h3>
                  <p className="text-xs text-stone-500">{def.playerCountLabel}</p>
                </div>
                <p className="text-[13px] text-stone-600 leading-relaxed">{def.description}</p>
              </li>
            );
          })}
        </ul>
      </section>

      {/* Bots */}
      <section aria-labelledby="bots-heading" className={sectionGrid}>
        <div className="lg:col-span-4">
          <h2 id="bots-heading" className={sectionHeading}>
            Bots that fill a seat, not a role
          </h2>
        </div>
        <div className="lg:col-span-8 grid grid-cols-1 md:grid-cols-[minmax(0,1.3fr)_minmax(0,1fr)] gap-10">
          <div className="flex flex-col gap-4 text-[15px] text-stone-600 leading-relaxed max-w-[58ch]">
            <p>
              Hearts needs four players and a group of three shouldn’t have to wait. The host can add a bot to
              any empty seat in the waiting room.
            </p>
            <p>
              A bot is an ordinary player: it connects to the room the same way your browser does and sees
              only its own cards. On each turn it weighs the moves the rules allow — using Jev, TypeSafe’s
              decision model — and plays the strongest one.
            </p>
          </div>
          <dl className="flex flex-col divide-y divide-stone-200 border-y border-stone-200 text-[13px] self-start">
            <div className="py-3 flex flex-col gap-0.5">
              <dt className="text-stone-500">Moves it considers</dt>
              <dd className="text-stone-900 font-medium">Only the legal moves the server lists</dd>
            </div>
            <div className="py-3 flex flex-col gap-0.5">
              <dt className="text-stone-500">If the model is slow</dt>
              <dd className="text-stone-900 font-medium">Plays a built-in move after 3 seconds</dd>
            </div>
            <div className="py-3 flex flex-col gap-0.5">
              <dt className="text-stone-500">What it can see</dt>
              <dd className="text-stone-900 font-medium">Its own hand and the table, like you</dd>
            </div>
          </dl>
        </div>
      </section>

      {/* Principles */}
      <section aria-labelledby="principles-heading" className={sectionGrid}>
        <div className="lg:col-span-4">
          <h2 id="principles-heading" className={sectionHeading}>
            What stays out of the game
          </h2>
        </div>
        <dl className="lg:col-span-8 grid grid-cols-1 sm:grid-cols-2 gap-x-10 gap-y-8">
          {PRINCIPLES.map(([term, detail]) => (
            <div key={term} className="flex flex-col gap-1.5 border-t border-stone-900 pt-4">
              <dt className="font-display text-base font-semibold text-stone-900">{term}</dt>
              <dd className="text-[15px] text-stone-600 leading-relaxed">{detail}</dd>
            </div>
          ))}
        </dl>
      </section>

      {/* Call to action */}
      <section className="mt-4 rounded-xl bg-stone-900 text-stone-50 px-6 sm:px-10 py-10 flex flex-col sm:flex-row sm:items-center justify-between gap-6">
        <div className="flex flex-col gap-1.5">
          <h2 className="font-display text-2xl font-semibold">Ready to deal?</h2>
          <p className="text-[15px] text-stone-400">Open a table in a few seconds and send the link.</p>
        </div>
        <a
          href="/"
          className="h-11 px-5 rounded-md bg-stone-50 text-stone-900 text-sm font-medium flex items-center justify-center gap-1.5 hover:bg-white active:scale-[0.99] transition-[background-color,transform] shrink-0"
        >
          Choose a game
          <span aria-hidden className="material-symbols-outlined text-[18px]">arrow_forward</span>
        </a>
      </section>
    </article>
  );
};
