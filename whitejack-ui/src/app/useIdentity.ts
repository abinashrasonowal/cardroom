import { useCallback, useEffect, useState } from 'react';
import { Me, getMe } from '@/network/api';

/**
 * Who this browser is to the server. The first request mints the identity cookie, and the
 * socket needs it before any join, so it is fetched on mount rather than on first use.
 */
export function useIdentity() {
  const [me, setMe] = useState<Me | null>(null);

  useEffect(() => {
    getMe().then(setMe).catch(() => setMe(null));
  }, []);

  /** The identity now, fetching it if the mount-time request has not landed (or failed). */
  const ensureMe = useCallback(async () => {
    if (me) return me;
    const fresh = await getMe();
    setMe(fresh);
    return fresh;
  }, [me]);

  return { me, ensureMe };
}
