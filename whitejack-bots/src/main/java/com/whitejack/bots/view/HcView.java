package com.whitejack.bots.view;

import com.whitejack.contract.Card;
import java.util.List;

/** games/high-card HcView. */
public record HcView(List<SeatView> seats, boolean handComplete, String winner, String onClock) {

    public record SeatView(int index, String id, String nick, boolean hasDrawn, Card card) {}
}
