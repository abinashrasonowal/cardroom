package com.whitejack.bots.view;

import com.whitejack.contract.Card;
import java.util.List;

/** games/gin-rummy GinView; fields a bot does not use are dropped on read. */
public record GinView(
        String phase,
        int hand,
        String onClock,
        List<SeatView> seats,
        List<Card> myHand,
        Arrangement myMelds,
        int stockCount,
        Card discardTop,
        int discardCount,
        Card takenFromDiscard,
        List<String> drawSources,
        List<Card> discards,
        List<Card> knockDiscards,
        List<Card> ginDiscards,
        String winner) {

    public record SeatView(int index, String id, String nick, int cardCount, int score) {}

    public record Arrangement(List<List<Card>> melds, List<Card> deadwood, int deadwoodPoints) {}
}
