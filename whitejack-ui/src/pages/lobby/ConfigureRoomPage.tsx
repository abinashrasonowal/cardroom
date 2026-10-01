import React, { useState } from 'react';
import { CardFan, suitFromGlyph } from '@/components/CardFan';
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
  'flex items-center gap-3 text-sm text-stone-700 py-2.5 cursor-pointer hover:text-stone-900 select-none';
const checkbox = 'h-4 w-4 rounded border-stone-300 accent-stone-900 cursor-pointer';
const sectionTitle = 'font-display text-base font-semibold text-stone-900';
const fieldLabel = 'text-[13px] font-medium text-stone-700';

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
    { label: 'Players', value: def.playerCountLabel },
    def.targetScore ? { label: 'Plays to', value: def.targetScore } : null,
    def.special ? { label: 'Signature', value: def.special } : null,
  ].filter((s): s is { label: string; value: string } => s !== null);

  return (
    <div className="w-full max-w-[1200px] mx-auto px-4 sm:px-6 flex flex-col gap-8 animate-rise">
      <nav aria-label="Breadcrumb">
        <button
          type="button"
          onClick={onBack}
          className="flex items-center gap-1 -ml-1.5 px-1.5 py-1 rounded-md text-[13px] font-medium text-stone-500 hover:text-stone-900 hover:bg-stone-100 transition-colors cursor-pointer"
        >
          <span aria-hidden className="material-symbols-outlined text-[18px]">arrow_back</span>
          All games
        </button>
      </nav>

      {/* Game header */}
      <header className="flex flex-col sm:flex-row sm:items-center gap-6 pb-8 border-b border-stone-200">
        <div className="shrink-0 rounded-xl bg-stone-100 w-32 h-32 flex items-center justify-center">
          <CardFan suit={suitFromGlyph(def.suitGlyph)} />
        </div>

        <div className="min-w-0 flex flex-col gap-2">
          {def.serverGameId && (
            <span className="flex items-center gap-1.5 text-[13px] font-medium text-felt-700">
              <span aria-hidden className="h-1.5 w-1.5 rounded-full bg-felt-500" />
              Online multiplayer
            </span>
          )}
          <h1 className="font-display text-3xl sm:text-4xl font-semibold text-stone-900 leading-tight">
            {def.name}
          </h1>
          <p className="text-[15px] text-stone-600 leading-relaxed max-w-[60ch]">{def.description}</p>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1.15fr)_minmax(0,1fr)] gap-10 lg:gap-14 items-start">
        {/* Rules & scoring */}
        <section aria-labelledby="rules-heading" className="flex flex-col gap-5">
          <h2 id="rules-heading" className={sectionTitle}>
            How it plays
          </h2>

          <p className="text-sm text-stone-600 leading-[1.7] max-w-[65ch]">{def.rulesOverview}</p>

          <dl className="grid grid-flow-col auto-cols-fr border-y border-stone-200 divide-x divide-stone-200">
            {stats.map((stat) => (
              <div key={stat.label} className="py-3 px-3 first:pl-0 min-w-0">
                <dt className="text-xs text-stone-500 truncate">{stat.label}</dt>
                <dd className="text-sm font-medium text-stone-900 truncate tabular mt-0.5">{stat.value}</dd>
              </div>
            ))}
          </dl>
        </section>

        {/* Room setup */}
        <section
          aria-labelledby="setup-heading"
          className="bg-white border border-stone-200 rounded-xl p-5 sm:p-6 shadow-raised flex flex-col gap-5"
        >
          <h2 id="setup-heading" className={sectionTitle}>
            Set up your room
          </h2>

          <label className="flex flex-col gap-1.5">
            <span className={fieldLabel}>Your name</span>
            <input
              type="text"
              name="nickname"
              autoComplete="nickname"
              spellCheck={false}
              maxLength={24}
              value={hostName}
              onChange={(e) => onHostNameChange(e.target.value)}
              placeholder="Julian…"
              className="w-full bg-white border border-stone-300 hover:border-stone-400 focus:border-stone-900 focus:ring-2 focus:ring-stone-900/10 rounded-md px-3 h-10 text-sm text-stone-900 placeholder:text-stone-400 outline-none transition-[border-color,box-shadow]"
            />
          </label>

          {/* Advanced options */}
          <div className="border-t border-stone-200 -mx-5 sm:-mx-6 px-5 sm:px-6">
            <button
              type="button"
              onClick={() => setAdvancedOpen((open) => !open)}
              aria-expanded={advancedOpen}
              aria-controls="advanced-options"
              className="w-full flex items-center justify-between py-3 text-sm font-medium text-stone-700 hover:text-stone-900 cursor-pointer"
            >
              <span>Advanced options</span>
              <span
                aria-hidden
                className={`material-symbols-outlined text-[20px] text-stone-400 transition-transform ${
                  advancedOpen ? 'rotate-180' : ''
                }`}
              >
                expand_more
              </span>
            </button>

            {advancedOpen && (
              <div id="advanced-options" className="pb-4 flex flex-col gap-3">
                {!def.serverGameId && def.rule1 && (
                  <fieldset className="flex flex-col divide-y divide-stone-100">
                    <legend className={`${fieldLabel} mb-1`}>House rules</legend>
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
                  </fieldset>
                )}

                <div className="flex items-center justify-between gap-3 bg-stone-50 px-3 py-2 rounded-md border border-stone-200">
                  <span className="text-[13px] text-stone-500 shrink-0">Room code</span>
                  <div className="flex items-center gap-2 min-w-0">
                    <code className="text-[13px] font-mono-code text-stone-900 truncate">
                      {def.serverGameId ? 'Assigned when created' : def.defaultCode}
                    </code>
                    {!def.serverGameId && (
                      <button
                        type="button"
                        onClick={handleCopyCode}
                        aria-label="Copy room code"
                        className="h-7 w-7 rounded flex items-center justify-center text-stone-400 hover:text-stone-900 hover:bg-stone-200 transition-colors cursor-pointer"
                      >
                        <span aria-hidden className="material-symbols-outlined text-[16px]">
                          {copied ? 'check' : 'content_copy'}
                        </span>
                      </button>
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>

          <div className="flex flex-col gap-2">
            <button
              type="button"
              onClick={handleLaunch}
              disabled={liveBusy}
              className="w-full h-11 rounded-md bg-stone-900 text-white text-sm font-medium flex items-center justify-center gap-1.5 hover:bg-stone-800 transition-[background-color,transform] cursor-pointer active:scale-[0.99] disabled:opacity-60 disabled:cursor-wait"
            >
              {liveBusy ? 'Creating room…' : 'Create room'}
              {!liveBusy && <span aria-hidden className="material-symbols-outlined text-[18px]">arrow_forward</span>}
            </button>
            <p aria-live="polite" className="text-[13px] min-h-5">
              {liveError ? (
                <span className="text-red-700">{liveError}. Check your connection and try again.</span>
              ) : (
                <span className="text-stone-500">You’ll get a link to share once the room is open.</span>
              )}
            </p>
          </div>
        </section>
      </div>
    </div>
  );
};
