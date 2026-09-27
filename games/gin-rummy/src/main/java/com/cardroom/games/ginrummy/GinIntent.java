package com.cardroom.games.ginrummy;

import com.cardroom.contract.Card;
import com.cardroom.contract.Intent;

/**
 * A turn is two moves: draw, then discard. Wire forms: {@code {"type":"draw","source":"stock"}}
 * and {@code {"type":"discard","card":{rank,suit},"knock":false}}. Knocking — and gin, which is
 * a knock with no deadwood — is a flag on the discard, because that is when it happens.
 */
public sealed interface GinIntent extends Intent {

    record Draw(DrawSource source) implements GinIntent {}

    record Discard(Card card, boolean knock) implements GinIntent {}
}
