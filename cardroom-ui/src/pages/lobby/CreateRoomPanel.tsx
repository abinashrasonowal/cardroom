import React, { useState } from 'react';
import { GameDefinition, RoomRules } from '@/types/game';
import { soundFx } from '@/utils/audio';

interface CreateRoomPanelProps {
  def: GameDefinition;
  rules: RoomRules;
  onToggleRule: (ruleIndex: 1 | 2 | 3) => void;
  hostName: string;
  onHostNameChange: (name: string) => void;
  /** Opens the room. A rejection is shown under the launch button. */
  onLaunch: () => Promise<void>;
}

/** The right-hand panel: rules for the selected game, house-rule toggles, and launch. */
export const CreateRoomPanel: React.FC<CreateRoomPanelProps> = ({
  def,
  rules,
  onToggleRule,
  hostName,
  onHostNameChange,
  onLaunch,
}) => {
  const [copied, setCopied] = useState<boolean>(false);
  const [copiedLinkNotice, setCopiedLinkNotice] = useState<boolean>(false);
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
      setCopiedLinkNotice(true);
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

  return (
    <div className="bg-slate-50 border border-slate-200 rounded-2xl p-6 flex flex-col gap-4 shadow-sm relative overflow-hidden">
      <div className="absolute -right-16 -top-16 w-64 h-64 bg-blue-600/5 rounded-full blur-3xl pointer-events-none" />

      <div className="flex items-center justify-between relative z-10">
        <div className="flex items-center gap-2">
          <span className="material-symbols-outlined text-blue-600 text-xl">add_circle</span>
          <h2 className="text-lg font-bold text-slate-950 font-space">Create & Rules</h2>
        </div>
        <span className="text-xs text-blue-700 bg-blue-100 border border-blue-200 px-2.5 py-0.5 rounded-full font-semibold">
          {def.shortName}
        </span>
      </div>

      {/* Rules Summary Box */}
      <div className="bg-white border border-slate-200 rounded-xl p-4 flex flex-col gap-2 relative z-10 shadow-sm">
        <div className="flex items-center justify-between text-xs uppercase font-semibold text-slate-500">
          <span className="flex items-center gap-1.5 text-slate-700 font-bold">
            <span className="material-symbols-outlined text-sm text-red-600">menu_book</span>
            Game Rules & Scoring
          </span>
          <span className="text-blue-600 font-mono-code text-xs font-semibold">
            {def.tag}
          </span>
        </div>
        <p className="text-xs sm:text-sm text-slate-600 leading-relaxed">
          {def.rulesOverview}
        </p>
      </div>

      <div className="flex flex-col gap-3.5 relative z-10">
        {/* Host Display Name Input */}
        <div className="flex flex-col gap-1.5">
          <label className="text-xs text-slate-700 uppercase font-semibold">
            Your Display Name (Guest Host)
          </label>
          <input
            type="text"
            value={hostName}
            onChange={(e) => onHostNameChange(e.target.value)}
            placeholder="e.g. Julian"
            className="w-full bg-white border border-slate-300 focus:border-blue-600 focus:ring-1 focus:ring-blue-600 rounded-lg px-3.5 h-11 text-sm text-slate-900 placeholder:text-slate-400 transition-colors shadow-sm"
          />
        </div>

        {/* Room Rule Toggles */}
        {!def.serverGameId && def.rule1 && (
        <div className="flex flex-col gap-1.5">
          <label className="text-xs text-slate-700 uppercase font-semibold">
            Room Rule Toggles
          </label>
          <div className="flex flex-col gap-2">
            <label className="flex items-center gap-2.5 text-slate-700 text-xs sm:text-sm bg-white px-3.5 py-2.5 rounded-lg border border-slate-200 cursor-pointer hover:text-slate-950 hover:border-slate-300 transition-colors shadow-sm select-none">
              <input
                type="checkbox"
                checked={rules.rule1}
                onChange={() => onToggleRule(1)}
                className="rounded border-slate-300 text-blue-600 focus:ring-0 focus:ring-offset-0 bg-white"
              />
              <span>{def.rule1}</span>
            </label>

            <label className="flex items-center gap-2.5 text-slate-700 text-xs sm:text-sm bg-white px-3.5 py-2.5 rounded-lg border border-slate-200 cursor-pointer hover:text-slate-950 hover:border-slate-300 transition-colors shadow-sm select-none">
              <input
                type="checkbox"
                checked={rules.rule2}
                onChange={() => onToggleRule(2)}
                className="rounded border-slate-300 text-blue-600 focus:ring-0 focus:ring-offset-0 bg-white"
              />
              <span>{def.rule2}</span>
            </label>

            <label className="flex items-center gap-2.5 text-slate-700 text-xs sm:text-sm bg-white px-3.5 py-2.5 rounded-lg border border-slate-200 cursor-pointer hover:text-slate-950 hover:border-slate-300 transition-colors shadow-sm select-none">
              <input
                type="checkbox"
                checked={rules.rule3}
                onChange={() => onToggleRule(3)}
                className="rounded border-slate-300 text-blue-600 focus:ring-0 focus:ring-offset-0 bg-white"
              />
              <span>{def.rule3}</span>
            </label>
          </div>
        </div>
        )}
      </div>

      {/* Launch Section */}
      <div className="pt-3 border-t border-slate-200 flex flex-col gap-3 relative z-10">
        <div className="flex items-center justify-between bg-white px-3.5 py-2 rounded-lg border border-slate-200 shadow-sm">
          <div className="flex items-center gap-2">
            <span className="text-xs text-slate-500 uppercase font-semibold">
              Room Code Preview:
            </span>
            <code className="text-sm font-mono-code text-blue-600 font-bold tracking-wider">
              {def.serverGameId ? 'Assigned on launch' : def.defaultCode}
            </code>
          </div>
          {!def.serverGameId && (
          <button
            type="button"
            onClick={handleCopyCode}
            className="material-symbols-outlined text-sm text-slate-400 hover:text-blue-600 transition-colors cursor-pointer"
            title="Copy Room Code"
          >
            {copied ? 'done' : 'content_copy'}
          </button>
          )}
        </div>

        <button
          type="button"
          onClick={handleLaunch}
          disabled={liveBusy}
          className="w-full py-3.5 rounded-xl bg-blue-600 text-white text-xs font-bold uppercase tracking-wider flex items-center justify-center gap-2 hover:bg-blue-700 transition-all duration-150 shadow-md cursor-pointer border border-blue-600 active:scale-[0.99]"
        >
          <span className="material-symbols-outlined text-lg">rocket_launch</span>
          <span>{liveBusy ? 'Opening room…' : 'Launch Room & Invite Friends'}</span>
        </button>
        {liveError && <p className="text-xs text-red-700 px-1">{liveError}</p>}

        <div className="flex items-center justify-between text-xs text-slate-500 px-1">
          <span className="text-xs">
            {def.serverGameId
              ? 'Invite link on the table screen'
              : copiedLinkNotice
                ? '✓ Link copied to clipboard!'
                : 'Instant link copied upon launch'}
          </span>
          <span className="text-xs text-blue-600 font-semibold flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-blue-600"></span>
            Zero signup
          </span>
        </div>
      </div>
    </div>
  );
};
