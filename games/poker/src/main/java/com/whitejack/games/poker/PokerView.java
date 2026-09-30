package com.whitejack.games.poker;

import com.whitejack.contract.Card;
import com.whitejack.contract.PlayerId;
import com.whitejack.contract.PlayerView;
import java.util.List;

/**
 * What one viewer may see: their own hole cards, never anyone else's until a showdown puts
 * them in {@code lastHand}. The viewer's betting options are computed here so the client and
 * the bots offer exactly the legal moves without carrying the rules (§3 rule 5).
 *
 * @param board the community cards face up now
 * @param pot every chip committed this hand, blinds and current-street bets included
 * @param legal the moves the viewer may make now ("fold", "check", "call", "raise"); empty when it is not their move
 * @param toCall chips the viewer must add to call; 0 when they can check
 * @param minRaiseTo the smallest street total the viewer may raise to, or 0 if they cannot raise
 * @param maxRaiseTo the viewer's all-in street total, or 0 if they cannot raise
 * @param lastHand how the previous hand ended; null before the first one finishes
 * @param winner who holds every chip once the game is over, else null
 */
public record PokerView(
        PokerPhase phase,
        Street street,
        int hand,
        int smallBlind,
        int bigBlind,
        PlayerId dealer,
        List<SeatView> seats,
        List<Card> myCards,
        List<Card> board,
        int pot,
        int currentBet,
        PlayerId onClock,
        List<String> legal,
        int toCall,
        int minRaiseTo,
        int maxRaiseTo,
        HandResult lastHand,
        PlayerId winner) implements PlayerView {

    /**
     * @param inHand dealt into the current hand (a player with no chips sits out)
     * @param bet chips put in on the current street
     * @param lastAction latest action on this street, e.g. "Raise to 120"; null if none yet
     */
    public record SeatView(int index, PlayerId id, String nick, int stack, int bet, boolean inHand, boolean folded,
            boolean allIn, String lastAction) {}
}
