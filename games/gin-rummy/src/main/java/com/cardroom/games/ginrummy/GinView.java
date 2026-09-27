package com.cardroom.games.ginrummy;

import com.cardroom.contract.Card;
import com.cardroom.contract.PlayerId;
import com.cardroom.contract.PlayerView;
import java.util.List;

/**
 * What one viewer may see: their own cards, the opponent's count, the top of the discard pile.
 *
 * <p>The viewer's legal moves are spelled out — {@code drawSources} while drawing,
 * {@code discards} / {@code knockDiscards} / {@code ginDiscards} while discarding — so neither
 * the UI nor a future bot needs the rules. A bot picks from these lists and cannot make an
 * illegal move.
 *
 * @param myMelds the viewer's hand arranged for least deadwood; null for a spectator
 * @param knockDiscards discards after which the viewer may knock (deadwood 10 or less)
 * @param ginDiscards discards that leave no deadwood at all
 */
public record GinView(
        GinPhase phase,
        int hand,
        PlayerId dealer,
        PlayerId onClock,
        List<SeatView> seats,
        List<Card> myHand,
        Melds.Arrangement myMelds,
        int stockCount,
        Card discardTop,
        int discardCount,
        Card takenFromDiscard,
        List<DrawSource> drawSources,
        List<Card> discards,
        List<Card> knockDiscards,
        List<Card> ginDiscards,
        HandResult lastHand,
        List<List<Integer>> history,
        PlayerId winner) implements PlayerView {

    public record SeatView(int index, PlayerId id, String nick, int cardCount, int score) {}
}
