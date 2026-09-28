import React from 'react';
import { TableSettings } from '@/types/game';
import { soundFx } from '@/utils/audio';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  settings: TableSettings;
  onUpdateSettings: (newSettings: TableSettings) => void;
}

export const SettingsModal: React.FC<SettingsModalProps> = ({
  isOpen,
  onClose,
  settings,
  onUpdateSettings,
}) => {
  if (!isOpen) return null;

  const handleToggleSound = () => {
    soundFx.playClick();
    const updated = !settings.soundEnabled;
    soundFx.enabled = updated;
    onUpdateSettings({ ...settings, soundEnabled: updated });
  };

  const handleVolumeChange = (vol: number) => {
    soundFx.volume = vol;
    onUpdateSettings({ ...settings, soundVolume: vol });
  };

  const handleSelectTheme = (theme: TableSettings['tableTheme']) => {
    soundFx.playClick();
    onUpdateSettings({ ...settings, tableTheme: theme });
  };

  const handleSelectCardBack = (back: TableSettings['cardBack']) => {
    soundFx.playCardDeal();
    onUpdateSettings({ ...settings, cardBack: back });
  };

  const handleToggleFourColor = () => {
    soundFx.playClick();
    onUpdateSettings({ ...settings, fourColorDeck: !settings.fourColorDeck });
  };

  const handleToggleSort = (sortBy: 'suit' | 'rank') => {
    soundFx.playClick();
    onUpdateSettings({ ...settings, sortBy });
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white border border-slate-200 rounded-2xl max-w-lg w-full p-6 shadow-2xl flex flex-col gap-5 animate-scale-up">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-200 pb-3">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-blue-600 text-xl">tune</span>
            <h3 className="text-base font-bold text-slate-950 font-space uppercase">
              Table & Card Preferences
            </h3>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-lg text-slate-400 hover:text-slate-800 hover:bg-slate-100 flex items-center justify-center text-lg transition-colors cursor-pointer"
          >
            ✕
          </button>
        </div>

        {/* Settings Form */}
        <div className="flex flex-col gap-4 text-xs sm:text-sm">
          {/* Table Felt Surface */}
          <div className="flex flex-col gap-2">
            <span className="text-xs text-slate-700 uppercase font-semibold">Table Felt Surface</span>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {[
                { id: 'slate', name: 'Tournament', color: 'bg-slate-900 border-slate-700' },
                { id: 'emerald', name: 'Emerald', color: 'bg-emerald-950 border-emerald-800' },
                { id: 'navy', name: 'Velvet Navy', color: 'bg-blue-950 border-blue-900' },
                { id: 'studio', name: 'Studio Light', color: 'bg-slate-100 border-slate-300' },
              ].map((t) => (
                <button
                  key={t.id}
                  onClick={() => handleSelectTheme(t.id as TableSettings['tableTheme'])}
                  className={`p-2 rounded-xl border flex flex-col items-center gap-1.5 transition-all cursor-pointer ${
                    settings.tableTheme === t.id
                      ? 'border-blue-600 ring-2 ring-blue-600/30'
                      : 'border-slate-200 hover:border-slate-300'
                  }`}
                >
                  <div className={`w-full h-8 rounded-lg ${t.color} border shadow-inner`} />
                  <span className="text-[11px] font-semibold text-slate-800">{t.name}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Card Back Style */}
          <div className="flex flex-col gap-2">
            <span className="text-xs text-slate-700 uppercase font-semibold">Card Back Design</span>
            <div className="grid grid-cols-3 gap-2">
              {[
                { id: 'geometric-blue', name: 'Royal Blue', desc: 'Lattice grid' },
                { id: 'classic-cross', name: 'Navy Cross', desc: 'Linen weave' },
                { id: 'crimson-diamond', name: 'Crimson', desc: 'Diamond dot' },
              ].map((b) => (
                <button
                  key={b.id}
                  onClick={() => handleSelectCardBack(b.id as TableSettings['cardBack'])}
                  className={`p-2.5 rounded-xl border text-left transition-all cursor-pointer ${
                    settings.cardBack === b.id
                      ? 'border-blue-600 bg-blue-50/50 ring-2 ring-blue-600/30'
                      : 'border-slate-200 hover:border-slate-300'
                  }`}
                >
                  <span className="text-xs font-bold text-slate-900 block">{b.name}</span>
                  <span className="text-[10px] text-slate-500 block">{b.desc}</span>
                </button>
              ))}
            </div>
          </div>

          {/* 4-Color Deck Toggle */}
          <div className="flex items-center justify-between p-3 bg-slate-50 border border-slate-200 rounded-xl">
            <div className="flex flex-col gap-0.5">
              <span className="text-xs font-bold text-slate-900">4-Color Tournament Deck</span>
              <span className="text-[11px] text-slate-500">
                ♠ Black · ♥ Red · ♦ Blue · ♣ Green for rapid suit recognition
              </span>
            </div>
            <button
              onClick={handleToggleFourColor}
              className={`w-11 h-6 flex items-center rounded-full p-1 transition-colors cursor-pointer ${
                settings.fourColorDeck ? 'bg-blue-600' : 'bg-slate-300'
              }`}
            >
              <div
                className={`bg-white w-4 h-4 rounded-full shadow-md transform transition-transform ${
                  settings.fourColorDeck ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
          </div>

          {/* Hand Sorting Preference */}
          <div className="flex items-center justify-between p-3 bg-slate-50 border border-slate-200 rounded-xl">
            <div className="flex flex-col gap-0.5">
              <span className="text-xs font-bold text-slate-900">Sort Hand By</span>
              <span className="text-[11px] text-slate-500">
                Organize cards in your hand automatically
              </span>
            </div>
            <div className="flex items-center gap-1 bg-white border border-slate-200 rounded-lg p-0.5">
              <button
                onClick={() => handleToggleSort('suit')}
                className={`px-2.5 py-1 rounded text-xs font-semibold cursor-pointer ${
                  settings.sortBy === 'suit' ? 'bg-blue-600 text-white' : 'text-slate-600'
                }`}
              >
                Suit
              </button>
              <button
                onClick={() => handleToggleSort('rank')}
                className={`px-2.5 py-1 rounded text-xs font-semibold cursor-pointer ${
                  settings.sortBy === 'rank' ? 'bg-blue-600 text-white' : 'text-slate-600'
                }`}
              >
                Rank
              </button>
            </div>
          </div>

          {/* Sound Controls */}
          <div className="flex flex-col gap-2 p-3 bg-slate-50 border border-slate-200 rounded-xl">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-900">Tactile Sound Effects</span>
              <button
                onClick={handleToggleSound}
                className={`w-11 h-6 flex items-center rounded-full p-1 transition-colors cursor-pointer ${
                  settings.soundEnabled ? 'bg-blue-600' : 'bg-slate-300'
                }`}
              >
                <div
                  className={`bg-white w-4 h-4 rounded-full shadow-md transform transition-transform ${
                    settings.soundEnabled ? 'translate-x-5' : 'translate-x-0'
                  }`}
                />
              </button>
            </div>

            {settings.soundEnabled && (
              <div className="flex items-center gap-3 pt-1">
                <span className="material-symbols-outlined text-sm text-slate-400">volume_down</span>
                <input
                  type="range"
                  min="0.1"
                  max="1.0"
                  step="0.05"
                  value={settings.soundVolume}
                  onChange={(e) => handleVolumeChange(parseFloat(e.target.value))}
                  className="flex-1 accent-blue-600 h-1 bg-slate-200 rounded"
                />
                <span className="material-symbols-outlined text-sm text-slate-600">volume_up</span>
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="pt-2 border-t border-slate-200 flex justify-end">
          <button
            onClick={onClose}
            className="px-5 py-2 rounded-xl bg-blue-600 text-white text-xs font-bold uppercase tracking-wider hover:bg-blue-700 transition-colors shadow-sm cursor-pointer"
          >
            Apply & Close
          </button>
        </div>
      </div>
    </div>
  );
};
