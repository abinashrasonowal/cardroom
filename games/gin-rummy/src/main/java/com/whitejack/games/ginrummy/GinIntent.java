package com.whitejack.games.ginrummy;

import com.whitejack.contract.Card;
import com.whitejack.contract.Intent;

/**
 * A turn is two moves: draw, then discard. Wire forms: {@code {"type":"draw","source":"stock"}}
 * and {@code {"type":"discard","card":{rank,suit},"knock":false}}. Knocking — and gin, which is
 * a knock with no deadwood — is a flag on the discard, because that is when it happens.
 */
public sealed interface GinIntent extends Intent {

    record Draw(DrawSource source) implements GinIntent {}

    record Discard(Card card, boolean knock) implements GinIntent {}
}
