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

/** games/hearts HeartsView. Other players' hands arrive only as `cardCount`. */
export type HeartsPhase = 'PASSING' | 'PLAYING' | 'SCORING' | 'GAME_OVER';
export type PassDirection = 'LEFT' | 'RIGHT' | 'ACROSS' | 'HOLD';

export interface HeartsSeat {
  index: number;
  id: string;
  nick: string;
  cardCount: number;
  handPoints: number;
  score: number;
  passed: boolean;
}

export interface HeartsPlay {
  player: string;
  card: WireCard;
}

export interface HeartsView {
  phase: HeartsPhase;
  hand: number;
  passDirection: PassDirection;
  heartsBroken: boolean;
  tricksPlayed: number;
  seats: HeartsSeat[];
  myHand: WireCard[];
  /** Cards you may pass or play right now, computed by the server; empty when it is not your move. */
  legal: WireCard[];
  trick: HeartsPlay[];
  leader: string | null;
  onClock: string | null;
  lastTrick: HeartsPlay[];
  lastTrickWinner: string | null;
  /** One row per finished hand, points in seat order. */
  history: number[][];
  winner: string | null;
}

/** `game` is the server module id ("high-card", "hearts"); it picks the board that renders `view`. */
type ViewFrame =
  | { viewType: 'lobby'; game: string; view: LobbyView }
  | { viewType: 'game'; game: string; view: unknown };

export type ServerFrame =
  | ({ v: 1; type: 'update'; room: string } & ViewFrame)
  | ({ v: 1; type: 'sync'; seq: number } & ViewFrame)
  | { v: 1; type: 'accepted'; re: string; seq: number }
  | { v: 1; type: 'rejected'; re: string | null; error: string; detail: string }
  | { v: 1; type: 'fault'; room: string; detail: string };
