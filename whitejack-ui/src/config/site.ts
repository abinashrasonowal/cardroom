/** Who built this and where the code lives — shown in the header, footer and About page. */
export const SITE_URL = 'https://whitejack.games/';
export const GITHUB_URL = 'https://github.com/abinashrasonowal/whitejack';
export const AUTHOR_NAME = 'Abinash Sonowal';
export const AUTHOR_URL = 'https://github.com/abinashrasonowal';
export const FEEDBACK_EMAIL = 'abinashrasonowal@gmail.com';

/** A path in the repository's main branch, for "read the code" links. */
export const sourceUrl = (path: string) => `${GITHUB_URL}/blob/main/${path}`;

/** A mailto link with the subject filled in and, at a table, the room code for context. */
export const feedbackHref = (room?: string) => {
  const subject = 'Whitejack feedback';
  const body = room ? `\n\n— sent from room ${room}` : '';
  return `mailto:${FEEDBACK_EMAIL}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
};
