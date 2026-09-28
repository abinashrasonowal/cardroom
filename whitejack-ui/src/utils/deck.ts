import { Card, GameKey, PlayedCard, Rank, Suit } from '@/types/game';

export const SUITS: Suit[] = ['clubs', 'diamonds', 'spades', 'hearts'];
export const RANKS: Rank[] = ['2', '3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K', 'A'];

export const SUIT_SYMBOLS: Record<Suit, string> = {
  hearts: '♥',
  spades: '♠',
  diamonds: '♦',
  clubs: '♣',
};

export const RANK_VALUES: Record<Rank, number> = {
  '2': 2,
  '3': 3,
  '4': 4,
  '5': 5,
  '6': 6,
  '7': 7,
  '8': 8,
  '9': 9,
  '10': 10,
  J: 11,
  Q: 12,
  K: 13,
  A: 14,
};

export function createFullDeck(): Card[] {
  const deck: Card[] = [];
  for (const suit of SUITS) {
    for (const rank of RANKS) {
      deck.push({
        id: `${rank}-${suit}`,
        suit,
        rank,
        value: RANK_VALUES[rank],
      });
    }
  }
  return deck;
}

export function createEuchreDeck(): Card[] {
  const euchreRanks: Rank[] = ['9', '10', 'J', 'Q', 'K', 'A'];
  const deck: Card[] = [];
  for (const suit of SUITS) {
    for (const rank of euchreRanks) {
      deck.push({
        id: `${rank}-${suit}`,
        suit,
        rank,
        value: RANK_VALUES[rank],
      });
    }
  }
  return deck;
}

// Fisher-Yates shuffle
export function shuffleDeck(deck: Card[]): Card[] {
  const arr = [...deck];
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

// Sort cards by suit order (Clubs, Diamonds, Spades, Hearts) then rank ascending
export function sortCards(cards: Card[], sortBy: 'suit' | 'rank' = 'suit'): Card[] {
  const suitOrder: Record<Suit, number> = { clubs: 0, diamonds: 1, spades: 2, hearts: 3 };

  return [...cards].sort((a, b) => {
    if (sortBy === 'suit') {
      if (suitOrder[a.suit] !== suitOrder[b.suit]) {
        return suitOrder[a.suit] - suitOrder[b.suit];
      }
      return a.value - b.value;
    } else {
      if (a.value !== b.value) {
        return a.value - b.value;
      }
      return suitOrder[a.suit] - suitOrder[b.suit];
    }
  });
}

// Check if card is the 2 of clubs
export function isTwoOfClubs(card: Card): boolean {
  return card.suit === 'clubs' && card.rank === '2';
}

// Check if card is Queen of Spades
export function isQueenOfSpades(card: Card): boolean {
  return card.suit === 'spades' && card.rank === 'Q';
}

// Check if card is Jack of Diamonds
export function isJackOfDiamonds(card: Card): boolean {
  return card.suit === 'diamonds' && card.rank === 'J';
}

// Hearts legal card play validator
export function isLegalPlayHearts(
  card: Card,
  hand: Card[],
  trick: PlayedCard[],
  isFirstTrick: boolean,
  heartsBroken: boolean
): { legal: boolean; reason?: string } {
  // 1. First card of round must be 2 of Clubs
  if (isFirstTrick && trick.length === 0) {
    if (isTwoOfClubs(card)) return { legal: true };
    return { legal: false, reason: 'You must lead the 2 of Clubs on the first trick.' };
  }

  // 2. If leading (trick.length === 0)
  if (trick.length === 0) {
    if (card.suit === 'hearts' && !heartsBroken) {
      const hasNonHearts = hand.some((c) => c.suit !== 'hearts');
      if (hasNonHearts) {
        return { legal: false, reason: 'Hearts have not been broken yet.' };
      }
    }
    return { legal: true };
  }

  // 3. Following suit
  const leadSuit = trick[0].card.suit;
  const hasLeadSuit = hand.some((c) => c.suit === leadSuit);

  if (hasLeadSuit) {
    if (card.suit === leadSuit) {
      return { legal: true };
    }
    return { legal: false, reason: `You must follow suit (${leadSuit.toUpperCase()}).` };
  }

  // 4. Void in lead suit - on first trick, cannot play point cards (Hearts or ♠Q) unless only have points
  if (isFirstTrick) {
    const isPointCard = card.suit === 'hearts' || isQueenOfSpades(card);
    if (isPointCard) {
      const hasNonPoints = hand.some((c) => c.suit !== 'hearts' && !isQueenOfSpades(c));
      if (hasNonPoints) {
        return { legal: false, reason: 'Cannot play penalty points (Hearts or ♠Q) on the first trick.' };
      }
    }
  }

  return { legal: true };
}

// Spades legal card play validator
export function isLegalPlaySpades(
  card: Card,
  hand: Card[],
  trick: PlayedCard[],
  spadesBroken: boolean
): { legal: boolean; reason?: string } {
  if (trick.length === 0) {
    if (card.suit === 'spades' && !spadesBroken) {
      const hasNonSpades = hand.some((c) => c.suit !== 'spades');
      if (hasNonSpades) {
        return { legal: false, reason: 'Spades have not been broken yet.' };
      }
    }
    return { legal: true };
  }

  const leadSuit = trick[0].card.suit;
  const hasLeadSuit = hand.some((c) => c.suit === leadSuit);

  if (hasLeadSuit) {
    if (card.suit === leadSuit) return { legal: true };
    return { legal: false, reason: `You must follow suit (${leadSuit.toUpperCase()}).` };
  }

  return { legal: true };
}

// Evaluate trick winner for standard trick-taking
export function determineTrickWinner(
  trick: PlayedCard[],
  trumpSuit?: Suit | null
): { winnerPlayerId: string; winningCard: Card } {
  if (trick.length === 0) throw new Error('Empty trick');

  const leadSuit = trick[0].card.suit;
  let winningPlay = trick[0];

  for (let i = 1; i < trick.length; i++) {
    const play = trick[i];

    if (trumpSuit && play.card.suit === trumpSuit) {
      // Trump card played
      if (winningPlay.card.suit !== trumpSuit) {
        winningPlay = play;
      } else if (play.card.value > winningPlay.card.value) {
        winningPlay = play;
      }
    } else if (winningPlay.card.suit !== trumpSuit && play.card.suit === leadSuit) {
      // Following lead suit and winning play is not trump
      if (play.card.value > winningPlay.card.value) {
        winningPlay = play;
      }
    }
  }

  return {
    winnerPlayerId: winningPlay.playerId,
    winningCard: winningPlay.card,
  };
}

// Calculate points in a Hearts trick
export function calculateHeartsTrickPoints(trick: PlayedCard[], omnibus: boolean = true): number {
  let points = 0;
  for (const { card } of trick) {
    if (card.suit === 'hearts') points += 1;
    if (isQueenOfSpades(card)) points += 13;
    if (omnibus && isJackOfDiamonds(card)) points -= 10;
  }
  return points;
}

// Simple but believable Bot AI for Hearts
export function chooseBotPlayHearts(
  hand: Card[],
  trick: PlayedCard[],
  isFirstTrick: boolean,
  heartsBroken: boolean
): Card {
  // Get all legal cards
  const legalCards = hand.filter(
    (card) => isLegalPlayHearts(card, hand, trick, isFirstTrick, heartsBroken).legal
  );

  if (legalCards.length === 0) return hand[0]; // Fallback
  if (legalCards.length === 1) return legalCards[0];

  // If first trick and leading, must be 2 of clubs
  if (isFirstTrick && trick.length === 0) {
    const twoClubs = legalCards.find(isTwoOfClubs);
    if (twoClubs) return twoClubs;
  }

  // If leading
  if (trick.length === 0) {
    // Prefer leading low cards from non-danger suits
    const safeCards = legalCards.filter((c) => !isQueenOfSpades(c) && c.suit !== 'hearts');
    if (safeCards.length > 0) {
      // pick lowest value safe card
      return safeCards.reduce((lowest, c) => (c.value < lowest.value ? c : lowest), safeCards[0]);
    }
    return legalCards.reduce((lowest, c) => (c.value < lowest.value ? c : lowest), legalCards[0]);
  }

  const leadSuit = trick[0].card.suit;
  const followingCards = legalCards.filter((c) => c.suit === leadSuit);

  if (followingCards.length > 0) {
    // Following suit
    const currentHighInTrick = trick
      .filter((p) => p.card.suit === leadSuit)
      .reduce((max, p) => (p.card.value > max ? p.card.value : max), 0);

    // Try to play under current high card to duck trick
    const ducks = followingCards.filter((c) => c.value < currentHighInTrick);
    if (ducks.length > 0) {
      // Play highest duck to get rid of high cards safely
      return ducks.reduce((highest, c) => (c.value > highest.value ? c : highest), ducks[0]);
    }

    // Must take trick or play lowest card
    return followingCards.reduce((lowest, c) => (c.value < lowest.value ? c : lowest), followingCards[0]);
  }

  // Void in lead suit! Excellent opportunity to dump bad cards
  // 1. Dump Queen of Spades if held and legal
  const queen = legalCards.find(isQueenOfSpades);
  if (queen) return queen;

  // 2. Dump high Hearts
  const hearts = legalCards.filter((c) => c.suit === 'hearts');
  if (hearts.length > 0) {
    return hearts.reduce((highest, c) => (c.value > highest.value ? c : highest), hearts[0]);
  }

  // 3. Dump highest card in hand (e.g. Ace/King of Spades)
  return legalCards.reduce((highest, c) => (c.value > highest.value ? c : highest), legalCards[0]);
}

// Bot AI for Spades
export function chooseBotPlaySpades(
  hand: Card[],
  trick: PlayedCard[],
  spadesBroken: boolean
): Card {
  const legalCards = hand.filter(
    (card) => isLegalPlaySpades(card, hand, trick, spadesBroken).legal
  );

  if (legalCards.length === 0) return hand[0];
  if (legalCards.length === 1) return legalCards[0];

  if (trick.length === 0) {
    // Lead lowest non-spade if possible
    const nonSpades = legalCards.filter((c) => c.suit !== 'spades');
    if (nonSpades.length > 0) {
      return nonSpades.reduce((lowest, c) => (c.value < lowest.value ? c : lowest), nonSpades[0]);
    }
    return legalCards[0];
  }

  const leadSuit = trick[0].card.suit;
  const followingCards = legalCards.filter((c) => c.suit === leadSuit);

  if (followingCards.length > 0) {
    // Follow suit: try to win with highest or play lowest
    return followingCards.reduce((highest, c) => (c.value > highest.value ? c : highest), followingCards[0]);
  }

  // Void in lead suit: trump with lowest spade or discard lowest trash card
  const spades = legalCards.filter((c) => c.suit === 'spades');
  if (spades.length > 0) {
    return spades.reduce((lowest, c) => (c.value < lowest.value ? c : lowest), spades[0]);
  }

  return legalCards.reduce((lowest, c) => (c.value < lowest.value ? c : lowest), legalCards[0]);
}
