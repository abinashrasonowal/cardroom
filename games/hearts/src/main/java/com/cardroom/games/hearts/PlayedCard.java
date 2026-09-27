package com.cardroom.games.hearts;

import com.cardroom.contract.Card;
import com.cardroom.contract.PlayerId;

/** One card in a trick, and who played it. Public: a trick is visible to the whole table. */
public record PlayedCard(PlayerId player, Card card) {}
