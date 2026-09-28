export type Suit = 'hearts' | 'spades' | 'diamonds' | 'clubs';
export type Rank = '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' | '10' | 'J' | 'Q' | 'K' | 'A';

export interface Card {
  id: string;
  suit: Suit;
  rank: Rank;
  value: number; // 2..14
}

export type GameKey = 'high_card' | 'hearts' | 'gin_rummy' | 'spades' | 'euchre' | 'oh_hell' | 'custom';

export type GameAccent = 'blue' | 'rose' | 'amber' | 'emerald' | 'violet' | 'orange' | 'slate';

export interface GameDefinition {
  key: GameKey;
  name: string;
  shortName: string;
  badge: string;
  playerCountLabel: string;
  playersCount: number;
  suitGlyph: string;
  /** Lobby card icon when it differs from the suit glyph (High Card shows "A♠"). */
  icon?: string;
  suitColor: string;
  /** Pastel colour family for the lobby card's background and its card-fan artwork. */
  accent: GameAccent;
  description: string;
  tag: string;
  rulesOverview: string;
  /** Winning threshold shown in the lobby's stat row; omitted for games without one. */
  targetScore?: string;
  /** The game's signature move, shown in the lobby's stat row. */
  special?: string;
  defaultCode: string;
  /**
   * The server module's id for a live game ("high-card", "hearts"). Set means the game runs on
   * whitejack-server and its room code is assigned at launch; unset means an offline demo.
   */
  serverGameId?: string;
  /** Shown in the lobby but not yet playable. */
  comingSoon?: boolean;
  /** House-rule toggles for offline demos. Live games use the server module's fixed rules. */
  rule1?: string;
  rule2?: string;
  rule3?: string;
}

export interface Player {
  id: string;
  name: string;
  avatar: string;
  isHost: boolean;
  isUser: boolean;
  cards: Card[];
  tricksWon: number;
  currentBid?: number | 'Nil' | 'Blind Nil';
  roundScore: number;
  totalScore: number;
  bags?: number;
}

export interface PlayedCard {
  card: Card;
  playerId: string;
  playerName: string;
}

export interface TableSettings {
  soundEnabled: boolean;
  soundVolume: number;
  tableTheme: 'slate' | 'emerald' | 'navy' | 'studio';
  cardBack: 'geometric-blue' | 'classic-cross' | 'crimson-diamond';
  fourColorDeck: boolean;
  sortBy: 'suit' | 'rank';
}

export interface RoomRules {
  rule1: boolean;
  rule2: boolean;
  rule3: boolean;
}

export interface GameLogEntry {
  id: string;
  text: string;
  type: 'play' | 'trick' | 'score' | 'system';
  timestamp: string;
}
