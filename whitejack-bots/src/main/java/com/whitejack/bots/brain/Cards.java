package com.whitejack.bots.brain;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.whitejack.contract.Card;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** Card on the wire and in a prompt. */
final class Cards {

    static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private Cards() {}

    static ObjectNode json(Card card) {
        return NODES.objectNode().put("rank", card.rank().name()).put("suit", card.suit().name());
    }

    static String list(Collection<Card> cards) {
        return cards == null || cards.isEmpty() ? "none" : cards.stream().map(Card::toString).collect(Collectors.joining(" "));
    }

    /** 0.4271 → "43%". */
    static String pct(double share) {
        return Math.round(share * 100) + "%";
    }

    static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
