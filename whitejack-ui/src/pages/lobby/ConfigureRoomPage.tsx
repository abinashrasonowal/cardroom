import React, { useState } from 'react';
import { CardFan, suitFromGlyph } from '@/components/CardFan';
import { ACCENTS } from '@/config/accents';
import { GameDefinition, RoomRules } from '@/types/game';
import { soundFx } from '@/utils/audio';

interface ConfigureRoomPageProps {
  def: GameDefinition;
  rules: RoomRules;
  onToggleRule: (ruleIndex: 1 | 2 | 3) => void;
  hostName: string;
  onHostNameChange: (name: string) => void;
  /** Opens the room. A rejection is shown under the launch button. */
  onLaunch: () => Promise<void>;
  onBack: () => void;
}

const ruleRow =
  'flex items-center gap-2.5 text-slate-700 text-xs sm:text-sm bg-white px-3.5 py-2.5 rounded-lg border border-slate-200 cursor-pointer hover:text-slate-950 hover:border-slate-300 transition-colors select-none';
const checkbox = 'rounded border-slate-300 text-violet-600 focus:ring-0 focus:ring-offset-0 bg-white';
const sectionTitle = 'flex items-center gap-2 text-sm text-slate-900 font-bold';
const card = 'bg-white border border-slate-200 rounded-2xl p-5 shadow-sm flex flex-col gap-4';

/** The page behind a game card's Play button: rules, house rules, host name, and launch. */
export const ConfigureRoomPage: React.FC<ConfigureRoomPageProps> = ({
  def,
  rules,
  onToggleRule,
  hostName,
  onHostNameChange,
  onLaunch,
  onBack,
}) => {
  const [copied, setCopied] = useState<boolean>(false);
  const [advancedOpen, setAdvancedOpen] = useState<boolean>(false);
  const [liveBusy, setLiveBusy] = useState<boolean>(false);
  const [liveError, setLiveError] = useState<string | null>(null);

  const accent = ACCENTS[def.accent];

  const handleCopyCode = () => {
    soundFx.playClick();
    navigator.clipboard?.writeText(def.defaultCode).catch(() => {});
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleLaunch = async () => {
    soundFx.playCardDeal();
    if (!def.serverGameId) {
      // Offline demo codes are fixed, so the invite link can be copied before launch.
      navigator.clipboard?.writeText(`${window.location.origin}/#${def.defaultCode}`).catch(() => {});
    }
    setLiveBusy(true);
    setLiveError(null);
    try {
      await onLaunch();
    } catch (e) {
      setLiveError(e instanceof Error ? e.message : 'Could not reach the server');
    } finally {
      setLiveBusy(false);
    }
  };

  const stats = [
    { icon: 'group', label: 'Players', value: def.playerCountLabel, tint: 'text-violet-500' },
    def.targetScore
      ? { icon: 'emoji_events', label: 'Target Score', value: def.targetScore, tint: 'text-amber-500' }
      : null,
    def.special
      ? { icon: 'workspace_premium', label: 'Special', value: def.special, tint: 'text-amber-500' }
      : null,
  ].filter((s): s is { icon: string; label: string; value: string; tint: string } => s !== null);

  return (
    <div className="w-full max-w-[1120px] mx-auto px-4 sm:px-8 flex flex-col gap-5">
      <button
        type="button"
        onClick={onBack}
        className="self-start flex items-center gap-1.5 text-sm font-semibold text-slate-600 hover:text-violet-700 transition-colors cursor-pointer"
      >
        <span className="material-symbols-outlined text-lg">arrow_back</span>
        Back to games
      </button>

      {/* Game header */}
      <header
        className={`rounded-2xl border p-5 sm:p-6 flex flex-col sm:flex-row sm:items-center gap-5 ${accent.surface}`}
      >
        <div className="relative flex items-center shrink-0">
          <span className={`absolute inset-0 m-auto w-20 h-20 rounded-full blur-2xl ${accent.glow}`} />
          <CardFan suit={suitFromGlyph(def.suitGlyph)} className="relative" />
        </div>

        <div className="min-w-0">
          <div className="flex items-center flex-wrap gap-1.5 mb-2">
            {def.serverGameId && (
              <span className="text-[10px] bg-emerald-600 text-white font-bold px-2 py-0.5 rounded-full">
                Live
              </span>
            )}
            <span className={`text-[10px] px-2 py-0.5 rounded-full font-semibold border ${accent.chip}`}>
              {def.playerCountLabel}
            </span>
            <span className={`text-[10px] px-2 py-0.5 rounded-full font-semibold border ${accent.chip}`}>
              {def.tag}
            </span>
          </div>
          <h1 className="font-space text-2xl sm:text-3xl font-bold text-slate-950 leading-tight">
            {def.name}
          </h1>
          <p className="text-sm text-slate-600 mt-1.5 leading-relaxed max-w-2xl">{def.description}</p>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5 items-start">
        {/* Rules & scoring */}
        <section className={card}>
          <div className="flex items-center justify-between gap-2">
            <span className={sectionTitle}>
              <span className="material-symbols-outlined text-base text-red-500">menu_book</span>
              Game Rules &amp; Scoring
            </span>
            <span className="text-[11px] text-violet-700 bg-violet-50 border border-violet-100 px-2.5 py-0.5 rounded-full font-semibold shrink-0">
              {def.shortName}
            </span>
          </div>

          <p className="text-xs sm:text-sm text-slate-600 leading-relaxed bg-slate-50 border border-slate-100 rounded-xl p-4">
            {def.rulesOverview}
          </p>

          <div className="flex items-stretch">
            {stats.map((stat, i) => (
              <div
                key={stat.label}
                className={`flex items-center gap-2 flex-1 min-w-0 ${
                  i > 0 ? 'border-l border-slate-200 pl-3' : ''
                }`}
              >
                <span className={`material-symbols-outlined text-lg shrink-0 ${stat.tint}`}>
                  {stat.icon}
                </span>
                <div className="min-w-0">
                  <div className="text-[10px] text-slate-400 uppercase font-semibold tracking-wide truncate">
                    {stat.label}
                  </div>
                  <div className="text-xs font-bold text-slate-900 truncate">{stat.value}</div>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Room setup */}
        <section className={card}>
          <span className={sectionTitle}>
            <span className="material-symbols-outlined text-base text-violet-600">settings</span>
            Room Settings
          </span>

          <div className="flex flex-col gap-1.5">
            <label className="text-[10px] text-slate-500 uppercase font-bold tracking-wide">
              Your Display Name (Guest Host)
            </label>
            <div className="relative">
              <span className="material-symbols-outlined text-base text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none">
                person
              </span>
              <input
                type="text"
                value={hostName}
                onChange={(e) => onHostNameChange(e.target.value)}
                placeholder="e.g. Julian"
                className="w-full bg-white border border-slate-300 focus:border-violet-600 focus:ring-1 focus:ring-violet-600 rounded-lg pl-10 pr-3 h-11 text-sm text-slate-900 placeholder:text-slate-400 outline-none transition-colors"
              />
            </div>
          </div>

          {/* Advanced options */}
          <div className="border border-slate-200 rounded-xl">
            <button
              type="button"
              onClick={() => setAdvancedOpen((open) => !open)}
              className="w-full flex items-center justify-between px-4 py-3 text-sm font-semibold text-slate-800 cursor-pointer"
            >
              <span>Advanced Options</span>
              <span
                className={`material-symbols-outlined text-lg text-slate-400 transition-transform ${
                  advancedOpen ? 'rotate-180' : ''
                }`}
              >
                expand_more
              </span>
            </button>

            {advancedOpen && (
              <div className="px-4 pb-4 flex flex-col gap-3 border-t border-slate-100 pt-3">
                {!def.serverGameId && def.rule1 && (
                  <div className="flex flex-col gap-2">
                    <label className="text-[10px] text-slate-500 uppercase font-bold tracking-wide">
                      Room Rule Toggles
                    </label>
                    <label className={ruleRow}>
                      <input type="checkbox" checked={rules.rule1} onChange={() => onToggleRule(1)} className={checkbox} />
                      <span>{def.rule1}</span>
                    </label>
                    <label className={ruleRow}>
                      <input type="checkbox" checked={rules.rule2} onChange={() => onToggleRule(2)} className={checkbox} />
                      <span>{def.rule2}</span>
                    </label>
                    <label className={ruleRow}>
                      <input type="checkbox" checked={rules.rule3} onChange={() => onToggleRule(3)} className={checkbox} />
                      <span>{def.rule3}</span>
                    </label>
                  </div>
                )}

                <div className="flex items-center justify-between bg-slate-50 px-3.5 py-2 rounded-lg border border-slate-200">
                  <div className="flex items-center gap-2 min-w-0">
                    <span className="text-[10px] text-slate-500 uppercase font-bold tracking-wide shrink-0">
                      Room Code
                    </span>
                    <code className="text-sm font-mono-code text-violet-600 font-bold tracking-wider truncate">
                      {def.serverGameId ? 'Assigned on launch' : def.defaultCode}
                    </code>
                  </div>
                  {!def.serverGameId && (
                    <button
                      type="button"
                      onClick={handleCopyCode}
                      className="material-symbols-outlined text-sm text-slate-400 hover:text-violet-600 transition-colors cursor-pointer"
                      title="Copy Room Code"
                    >
                      {copied ? 'done' : 'content_copy'}
                    </button>
                  )}
                </div>
              </div>
            )}
          </div>

          <button
            type="button"
            onClick={handleLaunch}
            disabled={liveBusy}
            className="w-full py-3.5 rounded-xl bg-violet-600 text-white text-sm font-bold flex items-center justify-center gap-2 hover:bg-violet-700 transition-all duration-150 shadow-md shadow-violet-200 cursor-pointer active:scale-[0.99] disabled:opacity-70"
          >
            <span className="material-symbols-outlined text-lg">add_circle</span>
            <span>{liveBusy ? 'Opening room…' : 'Create Room'}</span>
            {!liveBusy && <span className="material-symbols-outlined text-lg">arrow_forward</span>}
          </button>
          {liveError && <p className="text-xs text-red-700 px-1">{liveError}</p>}
        </section>
      </div>
    </div>
  );
};
