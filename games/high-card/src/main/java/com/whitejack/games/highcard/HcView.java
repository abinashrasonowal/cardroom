package com.whitejack.games.highcard;

import com.whitejack.contract.Card;
import com.whitejack.contract.PlayerId;
import com.whitejack.contract.PlayerView;
import java.util.List;

/**
 * What one viewer is allowed to see. A {@code null} card means "not yours, not revealed yet" —
 * the seat, the nickname and the fact that they have drawn are all public; the card is not.
 */
public record HcView(List<SeatView> seats, boolean handComplete, PlayerId winner, PlayerId onClock)
        implements PlayerView {

    public record SeatView(int index, PlayerId id, String nick, boolean hasDrawn, Card card) {}
}
