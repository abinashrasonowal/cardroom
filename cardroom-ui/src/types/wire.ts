// Mirrors of what cardroom-server puts on the wire (architecture §7). Views, never events.

export type Phase = 'LOBBY' | 'PLAYING' | 'FINISHED';

export interface LobbyMember {
  seat: number;
  id: string;
  nick: string;
  connected: boolean;
}

/** engine-core LobbyView: what everyone sees before the host starts. */
export interface LobbyView {
  room: string;
  gameId: string;
  phase: Phase;
  host: string | null;
  members: LobbyMember[];
}

export type WireRank =
  | 'TWO' | 'THREE' | 'FOUR' | 'FIVE' | 'SIX' | 'SEVEN' | 'EIGHT'
  | 'NINE' | 'TEN' | 'JACK' | 'QUEEN' | 'KING' | 'ACE';
export type WireSuit = 'CLUBS' | 'DIAMONDS' | 'HEARTS' | 'SPADES';

export interface WireCard {
  rank: WireRank;
  suit: WireSuit;
}

/** games/high-card HcView. A null card means "not yours, and not revealed yet". */
export interface HcSeat {
  index: number;
  id: string;
  nick: string;
  hasDrawn: boolean;
  card: WireCard | null;
}

export interface HcView {
  seats: HcSeat[];
  handComplete: boolean;
  winner: string | null;
  onClock: string | null;
}

type ViewFrame =
  | { viewType: 'lobby'; view: LobbyView }
  | { viewType: 'game'; view: unknown };

export type ServerFrame =
  | ({ v: 1; type: 'update'; room: string } & ViewFrame)
  | ({ v: 1; type: 'sync'; seq: number } & ViewFrame)
  | { v: 1; type: 'accepted'; re: string; seq: number }
  | { v: 1; type: 'rejected'; re: string | null; error: string; detail: string }
  | { v: 1; type: 'fault'; room: string; detail: string };
