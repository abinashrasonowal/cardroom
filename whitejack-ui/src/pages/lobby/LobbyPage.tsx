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
const LOBBY_GAMES: GameKey[] = ['high_card', 'hearts', 'gin_rummy', 'spades'];

export const LobbyPage: React.FC<LobbyPageProps> = ({
  onStartGame,
  onJoinRoom,
  onCreateLive,
  initialNickname = '',
}) => {
  /** Null while browsing games; the game being configured otherwise. */
  const [configuringKey, setConfiguringKey] = useState<GameKey | null>(null);
  const [hostName, setHostName] = useState<string>(initialNickname);
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
    const name = hostName.trim() || 'Host';
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
    <div className="w-full max-w-[1560px] mx-auto px-4 sm:px-8 flex flex-col gap-6">
      <QuickJoin onJoin={onJoinRoom} />

      <section className="bg-white border border-slate-200 rounded-2xl p-5 sm:p-6 shadow-sm flex flex-col gap-5">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex items-center gap-2.5">
            <span className="material-symbols-outlined text-violet-600 text-2xl">
              stadia_controller
            </span>
            <h2 className="font-space text-2xl sm:text-3xl font-bold text-slate-950">
              Select a Card Game
            </h2>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3.5">
          {games.map((def) => (
            <GameCard key={def.key} def={def} onOpen={() => handleOpenGame(def.key)} />
          ))}
        </div>
      </section>
    </div>
  );
};
