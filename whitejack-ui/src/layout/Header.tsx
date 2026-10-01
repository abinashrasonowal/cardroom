import React from 'react';

interface HeaderProps {
  soundEnabled: boolean;
  onToggleSound: () => void;
  /** True while the About page is showing, to mark it in the nav. */
  aboutActive?: boolean;
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

const iconButton =
  'h-9 w-9 rounded-md text-stone-500 hover:text-stone-900 hover:bg-stone-100 active:bg-stone-200 transition-colors cursor-pointer flex items-center justify-center shrink-0';

const navLink = (active: boolean) =>
  `h-8 px-2.5 rounded-md text-sm font-medium flex items-center transition-colors ${
    active ? 'text-stone-900' : 'text-stone-500 hover:text-stone-900 hover:bg-stone-100'
  }`;

/** The brand mark: a spade cut from an ink tile, matching the favicon. */
const Mark: React.FC = () => (
  <svg viewBox="0 0 32 32" aria-hidden className="h-7 w-7 shrink-0">
    <rect width="32" height="32" rx="7" className="fill-stone-900" />
    <path
      d="M16 7c-3.2 3.6-7 6-7 9.6A3.6 3.6 0 0 0 15 19.2c-.2 2-1 3.4-2.4 4.8h6.8c-1.4-1.4-2.2-2.8-2.4-4.8a3.6 3.6 0 0 0 6-2.6C23 13 19.2 10.6 16 7z"
      className="fill-stone-50"
    />
  </svg>
);

export const Header: React.FC<HeaderProps> = ({
  soundEnabled,
  onToggleSound,
  aboutActive = false,
  onOpenSettings,
  inGame = false,
  gameTitle,
  roomCode,
  onLeaveGame,
  liveRoom = false,
}) => {
  const brand = (
    <>
      <Mark />
      {/* In a live room a phone needs the width for the room controls; the mark alone stays. */}
      <span
        translate="no"
        className={`font-display text-[15px] font-semibold text-stone-900 ${liveRoom ? 'max-sm:hidden' : ''}`}
      >
        Whitejack
      </span>
    </>
  );

  return (
    <header className="fixed top-0 left-0 w-full z-40 bg-stone-50/85 backdrop-blur-md border-b border-stone-200/80">
      <div
        className={`h-14 w-full px-4 sm:px-6 flex items-center justify-between gap-4 mx-auto ${
          inGame ? 'max-w-[1440px]' : 'max-w-[1200px]'
        }`}
      >
        <div className="flex items-center gap-3 min-w-0">
          {inGame ? (
            <button
              type="button"
              onClick={onLeaveGame}
              aria-label="Back to lobby"
              className="flex items-center gap-2.5 rounded-md -mx-1 px-1 py-1 hover:bg-stone-100 transition-colors cursor-pointer"
            >
              {brand}
            </button>
          ) : (
            <a href="/" className="flex items-center gap-2.5 rounded-md -mx-1 px-1 py-1">
              {brand}
            </a>
          )}

          {inGame && roomCode && !liveRoom && (
            <div className="hidden md:flex items-center gap-2 pl-3 ml-1 border-l border-stone-200 text-sm">
              <span className="text-stone-500 capitalize">{gameTitle?.toLowerCase().replace('_', ' ')}</span>
              <code className="font-mono-code text-xs text-stone-700 bg-stone-100 px-1.5 py-0.5 rounded">
                {roomCode}
              </code>
            </div>
          )}
        </div>

        <div className="flex items-center gap-1 shrink-0">
          {!inGame && (
            <nav aria-label="Main" className="flex items-center gap-0.5 mr-2">
              <a href="/" aria-current={!aboutActive ? 'page' : undefined} className={navLink(!aboutActive)}>
                Games
              </a>
              <a href="#/about" aria-current={aboutActive ? 'page' : undefined} className={navLink(aboutActive)}>
                About
              </a>
            </nav>
          )}
          {liveRoom && <div id={ROOM_SLOT_ID} className="flex items-center" />}
          {inGame && !liveRoom && (
            <button
              type="button"
              onClick={onLeaveGame}
              className="h-9 px-3 mr-1 rounded-md text-sm font-medium text-stone-600 hover:text-stone-900 hover:bg-stone-100 transition-colors flex items-center gap-1.5 cursor-pointer"
            >
              <span aria-hidden className="material-symbols-outlined text-[18px]">arrow_back</span>
              <span className="hidden sm:inline">Lobby</span>
            </button>
          )}

          <button
            type="button"
            onClick={onToggleSound}
            className={iconButton}
            aria-label={soundEnabled ? 'Mute sound effects' : 'Unmute sound effects'}
            aria-pressed={!soundEnabled}
            title={soundEnabled ? 'Mute' : 'Unmute'}
          >
            <span aria-hidden className="material-symbols-outlined text-[20px]">
              {soundEnabled ? 'volume_up' : 'volume_off'}
            </span>
          </button>

          <button
            type="button"
            onClick={onOpenSettings}
            className={iconButton}
            aria-label="Table preferences"
            title="Preferences"
          >
            <span aria-hidden className="material-symbols-outlined text-[20px]">tune</span>
          </button>
        </div>
      </div>
    </header>
  );
};
