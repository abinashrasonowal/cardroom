import React, { useState } from 'react';
import { GAME_DEFINITIONS, DEFAULT_RULES } from '@/config/games';
import { GameKey, RoomRules } from '@/types/game';
import { soundFx } from '@/utils/audio';
import { ConfigureRoomPage } from './ConfigureRoomPage';
import { GameCard } from './GameCard';
import { QuickJoin } from './QuickJoin';

interface LobbyPageProps {
  onStartGame: (gameKey: GameKey, playerName: string, roomCode: string, rules: RoomRules) => void;
  onJoinRoom: (roomCode: string, nickname: string) => void;
  /** Opens a server-backed room for a game with a `serverGameId`. */
  onCreateLive: (serverGameId: string, nickname: string) => Promise<void>;
  initialNickname?: string;
}

/** Offline launches wait this long so the "link copied" notice is seen before the table opens. */
const OFFLINE_LAUNCH_DELAY_MS = 400;

/** The games listed in the lobby grid, in display order. */
const LOBBY_GAMES: GameKey[] = ['poker', 'hearts', 'gin_rummy', 'spades'];

/** Prefilled in the host name field, and used if it is left blank. */
const DEFAULT_HOST_NAME = 'Jack';

export const LobbyPage: React.FC<LobbyPageProps> = ({
  onStartGame,
  onJoinRoom,
  onCreateLive,
  initialNickname = '',
}) => {
  /** Null while browsing games; the game being configured otherwise. */
  const [configuringKey, setConfiguringKey] = useState<GameKey | null>(null);
  const [hostName, setHostName] = useState<string>(initialNickname || DEFAULT_HOST_NAME);
  const [rulesState, setRulesState] = useState<Record<GameKey, RoomRules>>(DEFAULT_RULES);

  const games = LOBBY_GAMES.map((key) => GAME_DEFINITIONS[key]);

  const handleOpenGame = (key: GameKey) => {
    if (GAME_DEFINITIONS[key].comingSoon) return;
    soundFx.playClick();
    setConfiguringKey(key);
    window.scrollTo({ top: 0 });
  };

  const handleToggleRule = (ruleIndex: 1 | 2 | 3) => {
    if (!configuringKey) return;
    soundFx.playClick();
    const rule = `rule${ruleIndex}` as keyof RoomRules;
    setRulesState((prev) => ({
      ...prev,
      [configuringKey]: { ...prev[configuringKey], [rule]: !prev[configuringKey][rule] },
    }));
  };

  const handleLaunch = async (key: GameKey) => {
    const def = GAME_DEFINITIONS[key];
    const name = hostName.trim() || DEFAULT_HOST_NAME;
    if (def.serverGameId) {
      await onCreateLive(def.serverGameId, name);
      return;
    }
    await new Promise((resolve) => setTimeout(resolve, OFFLINE_LAUNCH_DELAY_MS));
    onStartGame(key, name, def.defaultCode, rulesState[key]);
  };

  if (configuringKey) {
    return (
      <ConfigureRoomPage
        key={configuringKey}
        def={GAME_DEFINITIONS[configuringKey]}
        rules={rulesState[configuringKey]}
        onToggleRule={handleToggleRule}
        hostName={hostName}
        onHostNameChange={setHostName}
        onLaunch={() => handleLaunch(configuringKey)}
        onBack={() => {
          soundFx.playClick();
          setConfiguringKey(null);
        }}
      />
    );
  }

  return (
    <div className="w-full max-w-[1200px] mx-auto px-4 sm:px-6 flex flex-col gap-12 sm:gap-14">
      <QuickJoin onJoin={onJoinRoom} />

      <section aria-labelledby="games-heading" className="flex flex-col gap-4">
        <div className="flex items-baseline justify-between gap-4 border-b border-stone-200 pb-3">
          <h2 id="games-heading" className="font-display text-xl font-semibold text-stone-900">
            Start a table
          </h2>
          <span className="max-sm:hidden text-[13px] text-stone-500">Choose a game to set up a room</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {games.map((def, i) => (
            <GameCard key={def.key} def={def} index={i} onOpen={() => handleOpenGame(def.key)} />
          ))}
        </div>
      </section>
    </div>
  );
};
