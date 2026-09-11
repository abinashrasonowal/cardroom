package com.cardroom.games.highcard;

import com.cardroom.contract.Card;
import com.cardroom.contract.PlayerId;
import com.cardroom.contract.PlayerView;
import java.util.List;

/**
 * What one viewer is allowed to see. A {@code null} card means "not yours, not revealed yet" —
 * the seat, the nickname and the fact that they have drawn are all public; the card is not.
 */
public record HcView(List<SeatView> seats, boolean handComplete, PlayerId winner, PlayerId onClock)
        implements PlayerView {

    public record SeatView(int index, PlayerId id, String nick, boolean hasDrawn, Card card) {}
}
