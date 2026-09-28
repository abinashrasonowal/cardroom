import { Card, Rank, Suit } from '@/types/game';
import { WireCard, WireRank } from '@/types/wire';

const RANKS: Record<WireRank, [Rank, number]> = {
  TWO: ['2', 2], THREE: ['3', 3], FOUR: ['4', 4], FIVE: ['5', 5], SIX: ['6', 6],
  SEVEN: ['7', 7], EIGHT: ['8', 8], NINE: ['9', 9], TEN: ['10', 10],
  JACK: ['J', 11], QUEEN: ['Q', 12], KING: ['K', 13], ACE: ['A', 14],
};

/** Server card → the shape CardView already renders. */
export function toUiCard(card: WireCard): Card {
  const [rank, value] = RANKS[card.rank];
  const suit = card.suit.toLowerCase() as Suit;
  return { id: `${rank}-${suit}`, rank, suit, value };
}

/** Crockford base32, six characters — RoomRegistry's alphabet. */
export const ROOM_CODE = /^[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{6}$/;
