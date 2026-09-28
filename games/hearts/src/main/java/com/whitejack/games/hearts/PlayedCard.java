package com.whitejack.games.hearts;

import com.whitejack.contract.Card;
import com.whitejack.contract.PlayerId;

/** One card in a trick, and who played it. Public: a trick is visible to the whole table. */
public record PlayedCard(PlayerId player, Card card) {}
