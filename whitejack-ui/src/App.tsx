import React, { useState, useEffect } from 'react';
import { useIdentity } from '@/app/useIdentity';
import { Footer } from '@/layout/Footer';
import { Header } from '@/layout/Header';
import { AboutModal } from '@/modals/AboutModal';
import { SettingsModal } from '@/modals/SettingsModal';
import { createRoom } from '@/network/api';
import { LiveTable } from '@/pages/games/live/LiveTable';
import { GameTable } from '@/pages/games/offline/GameTable';
import { JoinInvite } from '@/pages/lobby/JoinInvite';
import { LobbyPage } from '@/pages/lobby/LobbyPage';
import { GameKey, RoomRules, TableSettings } from '@/types/game';
import { soundFx } from '@/utils/audio';
import { ROOM_CODE } from '@/utils/cards';
import { loadNick, saveNick } from '@/utils/storage';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState<'lobby' | 'game'>('lobby');
  const [activeGameKey, setActiveGameKey] = useState<GameKey>('hearts');
  const [activeRoomCode, setActiveRoomCode] = useState<string>('');
  const [activePlayerName, setActivePlayerName] = useState<string>('');
  const [activeRules, setActiveRules] = useState<RoomRules>({
    rule1: true,
    rule2: true,
    rule3: false,
  });

  const [settings, setSettings] = useState<TableSettings>({
    soundEnabled: true,
    soundVolume: 0.7,
    tableTheme: 'slate',
    cardBack: 'geometric-blue',
    fourColorDeck: false,
    sortBy: 'suit',
  });

  const [isAboutOpen, setIsAboutOpen] = useState<boolean>(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState<boolean>(false);

  const { me, ensureMe } = useIdentity();
  const [live, setLive] = useState<{ room: string; nick: string } | null>(null);
  /** A room code from an invite link, waiting for the visitor to say who they are. */
  const [invite, setInvite] = useState<string | null>(null);

  const enterLive = (room: string, nick: string) => {
    saveNick(nick);
    setLive({ room, nick });
    window.location.hash = room;
  };

  const handleCreateLive = async (gameId: string, nick: string) => {
    await ensureMe();
    enterLive(await createRoom(gameId), nick);
  };

  // Check URL hash for direct room code join: a six-character server code (#K7M2QX) opens the
  // live table; the older prefixed codes (#HRT-8429) still open the offline demos.
  useEffect(() => {
    const hash = window.location.hash.replace('#', '').trim().toUpperCase();
    if (ROOM_CODE.test(hash)) {
      setInvite(hash);
    } else if (hash && hash.length >= 4) {
      setActiveRoomCode(hash);
      if (hash.startsWith('SPD')) setActiveGameKey('spades');
      else if (hash.startsWith('ECH')) setActiveGameKey('euchre');
      else if (hash.startsWith('OHL')) setActiveGameKey('oh_hell');
      else if (hash.startsWith('BOX')) setActiveGameKey('custom');
      else setActiveGameKey('hearts');
    }
  }, []);

  const handleToggleSound = () => {
    const updated = !settings.soundEnabled;
    soundFx.enabled = updated;
    setSettings((prev) => ({ ...prev, soundEnabled: updated }));
    if (updated) {
      soundFx.playClick();
    }
  };

  const handleStartGame = (
    gameKey: GameKey,
    playerName: string,
    roomCode: string,
    rules: RoomRules
  ) => {
    setActiveGameKey(gameKey);
    setActivePlayerName(playerName);
    setActiveRoomCode(roomCode);
    setActiveRules(rules);
    setCurrentScreen('game');
    window.location.hash = roomCode;
  };

  const handleJoinRoom = (roomCode: string, nickname: string) => {
    const code = roomCode.toUpperCase();
    if (ROOM_CODE.test(code)) {
      enterLive(code, nickname);
      return;
    }
    let key: GameKey = 'hearts';
    if (code.startsWith('SPD')) key = 'spades';
    else if (code.startsWith('ECH')) key = 'euchre';
    else if (code.startsWith('OHL')) key = 'oh_hell';
    else if (code.startsWith('BOX')) key = 'custom';

    handleStartGame(key, nickname, code, {
      rule1: true,
      rule2: true,
      rule3: false,
    });
  };

  const handleLeaveTable = () => {
    soundFx.playClick();
    setLive(null);
    setInvite(null);
    setCurrentScreen('lobby');
    window.location.hash = '';
  };

  return (
    <div className="page-surface min-h-screen flex flex-col justify-between selection:bg-violet-600 selection:text-white font-sans">
      {/* Top Header */}
      <Header
        soundEnabled={settings.soundEnabled}
        onToggleSound={handleToggleSound}
        onOpenAbout={() => setIsAboutOpen(true)}
        onOpenSettings={() => setIsSettingsOpen(true)}
        inGame={live != null || currentScreen === 'game'}
        gameTitle={live ? 'LIVE ROOM' : activeGameKey.toUpperCase()}
        roomCode={live ? live.room : activeRoomCode}
        onLeaveGame={handleLeaveTable}
        liveRoom={live != null}
      />

      {/* Main Content Area */}
      <main className="w-full pt-28 pb-16 flex-1 flex flex-col">
        {live ? (
          me ? (
            <LiveTable
              room={live.room}
              nick={live.nick}
              playerId={me.playerId}
              token={me.token}
              settings={settings}
              onLeave={handleLeaveTable}
            />
          ) : (
            <p className="text-center text-sm text-slate-600">Connecting to the whitejack server…</p>
          )
        ) : invite ? (
          <JoinInvite
            room={invite}
            initialNickname={loadNick()}
            onJoin={(nick) => {
              setInvite(null);
              enterLive(invite, nick);
            }}
            onCancel={handleLeaveTable}
          />
        ) : currentScreen === 'lobby' ? (
          <LobbyPage
            initialNickname={activePlayerName}
            onStartGame={handleStartGame}
            onJoinRoom={handleJoinRoom}
            onCreateLive={handleCreateLive}
          />
        ) : (
          <GameTable
            gameKey={activeGameKey}
            roomCode={activeRoomCode}
            playerName={activePlayerName}
            rules={activeRules}
            settings={settings}
            onLeaveTable={handleLeaveTable}
            onOpenSettings={() => setIsSettingsOpen(true)}
          />
        )}
      </main>

      {/* Footer */}
      <Footer />

      {/* Modals */}
      <AboutModal isOpen={isAboutOpen} onClose={() => setIsAboutOpen(false)} />
      <SettingsModal
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
        settings={settings}
        onUpdateSettings={setSettings}
      />
    </div>
  );
}
