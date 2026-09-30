package com.whitejack.bots.view;

import com.whitejack.contract.Card;
import java.util.List;

/** games/poker PokerView; fields a bot does not use are dropped on read. */
public record PokerView(
        String phase,
        String street,
        int hand,
        int smallBlind,
        int bigBlind,
        String dealer,
        List<SeatView> seats,
        List<Card> myCards,
        List<Card> board,
        int pot,
        int currentBet,
        String onClock,
        List<String> legal,
        int toCall,
        int minRaiseTo,
        int maxRaiseTo,
        String winner) {

    public record SeatView(int index, String id, String nick, int stack, int bet, boolean inHand, boolean folded,
            boolean allIn, String lastAction) {}
}
