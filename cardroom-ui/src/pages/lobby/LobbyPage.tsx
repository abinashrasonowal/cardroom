import React, { useState } from 'react';
import { GAME_DEFINITIONS, DEFAULT_RULES } from '@/config/games';
import { GameKey, RoomRules } from '@/types/game';
import { soundFx } from '@/utils/audio';
import { CreateRoomPanel } from './CreateRoomPanel';
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

export const LobbyPage: React.FC<LobbyPageProps> = ({
  onStartGame,
  onJoinRoom,
  onCreateLive,
  initialNickname = 'Julian',
}) => {
  const [selectedGameKey, setSelectedGameKey] = useState<GameKey>('hearts');
  const [hostName, setHostName] = useState<string>(initialNickname);
  const [rulesState, setRulesState] = useState<Record<GameKey, RoomRules>>(DEFAULT_RULES);

  const activeDef = GAME_DEFINITIONS[selectedGameKey];
  const activeRules = rulesState[selectedGameKey];
  const games = Object.values(GAME_DEFINITIONS);

  const handleSelectGame = (key: GameKey) => {
    soundFx.playClick();
    setSelectedGameKey(key);
  };

  const handleToggleRule = (ruleIndex: 1 | 2 | 3) => {
    soundFx.playClick();
    const rule = `rule${ruleIndex}` as keyof RoomRules;
    setRulesState((prev) => ({
      ...prev,
      [selectedGameKey]: { ...prev[selectedGameKey], [rule]: !prev[selectedGameKey][rule] },
    }));
  };

  const handleLaunch = async () => {
    const name = hostName.trim() || 'Host';
    if (activeDef.serverGameId) {
      await onCreateLive(activeDef.serverGameId, name);
      return;
    }
    await new Promise((resolve) => setTimeout(resolve, OFFLINE_LAUNCH_DELAY_MS));
    onStartGame(selectedGameKey, name, activeDef.defaultCode, activeRules);
  };

  return (
    <div className="w-full max-w-[1560px] mx-auto px-4 sm:px-8 flex flex-col gap-6">
      <QuickJoin onJoin={onJoinRoom} />

      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 pt-2 border-b border-slate-200 pb-6">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-3xl sm:text-4xl text-slate-950 font-bold tracking-tight font-space">
            Select a Card Game
          </h1>
          <p className="text-sm sm:text-base text-slate-600 max-w-2xl">
            Choose a game to customize rules and create a private room, or enter a room code to join
            friends. Games marked Live run on the server; the rest are offline demos against bots.
          </p>
        </div>
      </div>

      {/* 7 columns of games, 5 of rules */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        <div className="lg:col-span-7 flex flex-col gap-3">
          <div className="flex items-center justify-between pb-1">
            <span className="text-xs text-slate-600 uppercase font-semibold tracking-wider">
              Available Game Modes ({games.length})
            </span>
            <span className="text-xs text-blue-600 font-medium">Click game to configure room</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
            {games.map((def, i) => (
              // An odd card out spans both columns rather than leaving a hole in the grid.
              <div key={def.key} className={games.length % 2 === 1 && i === games.length - 1 ? 'sm:col-span-2' : ''}>
                <GameCard
                  def={def}
                  selected={def.key === selectedGameKey}
                  onSelect={() => handleSelectGame(def.key)}
                />
              </div>
            ))}
          </div>
        </div>

        <div className="lg:col-span-5 flex flex-col gap-6">
          <CreateRoomPanel
            key={selectedGameKey}
            def={activeDef}
            rules={activeRules}
            onToggleRule={handleToggleRule}
            hostName={hostName}
            onHostNameChange={setHostName}
            onLaunch={handleLaunch}
          />
        </div>
      </div>
    </div>
  );
};
