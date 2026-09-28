package com.whitejack.games.hearts;

import com.whitejack.contract.Card;
import com.whitejack.contract.Intent;
import java.util.List;

/**
 * The two moves in Hearts. Wire forms: {@code {"type":"pass","cards":[{rank,suit},…]}} and
 * {@code {"type":"play","card":{rank,suit}}}.
 */
public sealed interface HeartsIntent extends Intent {

    record Pass(List<Card> cards) implements HeartsIntent {
        public Pass {
            cards = List.copyOf(cards);
        }
    }

    record Play(Card card) implements HeartsIntent {}
}
