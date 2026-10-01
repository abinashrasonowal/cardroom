import React, { useEffect, useId } from 'react';

interface DialogProps {
  title: string;
  description?: string;
  onClose: () => void;
  children: React.ReactNode;
  footer?: React.ReactNode;
}

/** Centered modal shell shared by the About and Preferences dialogs: Escape and backdrop close it. */
export const Dialog: React.FC<DialogProps> = ({ title, description, onClose, children, footer }) => {
  const titleId = useId();

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  return (
    <div
      className="fixed inset-0 z-50 bg-stone-900/40 backdrop-blur-[2px] flex items-center justify-center p-4 overscroll-contain"
      onMouseDown={(e) => e.target === e.currentTarget && onClose()}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="bg-white border border-stone-200 rounded-xl max-w-lg w-full max-h-[calc(100dvh-2rem)] overflow-y-auto shadow-overlay flex flex-col animate-scale-up"
      >
        <div className="flex items-start justify-between gap-4 px-6 pt-5 pb-4 border-b border-stone-200">
          <div className="flex flex-col gap-0.5">
            <h2 id={titleId} className="font-display text-base font-semibold text-stone-900">
              {title}
            </h2>
            {description && <p className="text-[13px] text-stone-500">{description}</p>}
          </div>
          <button
            type="button"
            onClick={onClose}
            autoFocus
            aria-label="Close"
            className="h-8 w-8 -mr-2 -mt-1 rounded-md text-stone-400 hover:text-stone-900 hover:bg-stone-100 flex items-center justify-center transition-colors cursor-pointer"
          >
            <span aria-hidden className="material-symbols-outlined text-[20px]">close</span>
          </button>
        </div>

        <div className="px-6 py-5">{children}</div>

        {footer && <div className="px-6 py-4 border-t border-stone-200 flex justify-end">{footer}</div>}
      </div>
    </div>
  );
};

export const dialogPrimaryButton =
  'h-9 px-4 rounded-md bg-stone-900 text-white text-sm font-medium hover:bg-stone-800 active:scale-[0.99] transition-[background-color,transform] cursor-pointer';
